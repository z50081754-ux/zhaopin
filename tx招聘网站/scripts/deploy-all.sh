#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SSH_HOST="${XW_SSH_HOST:-xw-lightsail}"
WEB_ROOT="${XW_WEB_ROOT:-/var/www/xw-web}"
ADMIN_ROOT="${XW_ADMIN_ROOT:-/var/www/xw-admin}"
BACKEND_ROOT="${XW_BACKEND_ROOT:-/opt/xw/backend}"
BACKUP_ROOT="${XW_BACKUP_ROOT:-/var/backups/xw-releases}"
JAR_NAME="recruitment-api-0.0.1-SNAPSHOT.jar"
RELEASE_ID="$(date +%Y%m%d-%H%M%S)"

LOCAL_STAGE="$(mktemp -d "${TMPDIR:-/tmp}/xw-all-${RELEASE_ID}.XXXXXX")"
WEB_ARCHIVE="$LOCAL_STAGE/xw-web.tar.gz"
ADMIN_ARCHIVE="$LOCAL_STAGE/xw-admin.tar.gz"
LOCAL_JAR="$PROJECT_DIR/backend/target/$JAR_NAME"
REMOTE_ROOT="/home/ubuntu/xw-release-$RELEASE_ID"

cleanup() {
  rm -rf "$LOCAL_STAGE"
}
trap cleanup EXIT

if [[ -x /usr/local/opt/node@22/bin/node ]]; then
  export PATH="/usr/local/opt/node@22/bin:$PATH"
fi

cd "$PROJECT_DIR"

echo "[1/7] Build website and admin"
npm run build:all

echo "[2/7] Test and package backend"
mvn -f backend/pom.xml clean package

echo "[3/7] Package static releases"
COPYFILE_DISABLE=1 tar -czf "$WEB_ARCHIVE" -C dist .
COPYFILE_DISABLE=1 tar -czf "$ADMIN_ARCHIVE" -C admin/dist .
test -f "$LOCAL_JAR"

echo "[4/7] Upload release"
ssh "$SSH_HOST" "mkdir -p '$REMOTE_ROOT'"
scp "$WEB_ARCHIVE" "$SSH_HOST:$REMOTE_ROOT/web.tar.gz"
scp "$ADMIN_ARCHIVE" "$SSH_HOST:$REMOTE_ROOT/admin.tar.gz"
scp "$LOCAL_JAR" "$SSH_HOST:$REMOTE_ROOT/$JAR_NAME"

echo "[5/7] Backup and deploy backend"
ssh "$SSH_HOST" bash -s -- "$RELEASE_ID" "$REMOTE_ROOT" "$BACKEND_ROOT" "$BACKUP_ROOT" "$JAR_NAME" <<'REMOTE_BACKEND'
set -Eeuo pipefail

release_id="$1"
remote_root="$2"
backend_root="$3"
backup_root="$4"
jar_name="$5"
backup_dir="$backup_root/$release_id"

sudo mkdir -p "$backup_dir/backend" "$backend_root/target"
sudo -u postgres pg_dump -Fc -f "/tmp/xw-recruitment-$release_id.dump" xw_recruitment
sudo mv "/tmp/xw-recruitment-$release_id.dump" "$backup_dir/xw-recruitment.dump"
if [[ -f "$backend_root/target/$jar_name" ]]; then
  sudo cp -a "$backend_root/target/$jar_name" "$backup_dir/backend/$jar_name"
fi

rollback_backend() {
  set +e
  sudo systemctl stop xw-recruitment
  if [[ -f "$backup_dir/backend/$jar_name" ]]; then
    sudo install -o ubuntu -g ubuntu -m 0644 "$backup_dir/backend/$jar_name" "$backend_root/target/$jar_name"
    sudo systemctl start xw-recruitment
  fi
}
trap rollback_backend ERR

sudo systemctl stop xw-recruitment
sudo install -o ubuntu -g ubuntu -m 0644 "$remote_root/$jar_name" "$backend_root/target/$jar_name"
sudo systemctl start xw-recruitment

healthy=0
for _ in {1..30}; do
  if curl --fail --silent --max-time 3 http://127.0.0.1:8080/api/site-settings >/dev/null; then
    healthy=1
    break
  fi
  sleep 1
done

if [[ "$healthy" != "1" ]]; then
  rollback_backend
  trap - ERR
  echo "Backend health check failed; previous JAR restored." >&2
  exit 1
fi
trap - ERR
REMOTE_BACKEND

echo "[6/7] Backup and publish website and admin"
ssh "$SSH_HOST" bash -s -- "$RELEASE_ID" "$REMOTE_ROOT" "$WEB_ROOT" "$ADMIN_ROOT" "$BACKUP_ROOT" <<'REMOTE_STATIC'
set -Eeuo pipefail

release_id="$1"
remote_root="$2"
web_root="$3"
admin_root="$4"
backup_root="$5"
backup_dir="$backup_root/$release_id"
web_stage="$remote_root/web"
admin_stage="$remote_root/admin"

sudo mkdir -p "$backup_dir/web" "$backup_dir/admin" "$web_stage" "$admin_stage" "$web_root" "$admin_root"
sudo rsync -a "$web_root/" "$backup_dir/web/"
sudo rsync -a "$admin_root/" "$backup_dir/admin/"

rollback_static() {
  set +e
  sudo rsync -a --delete "$backup_dir/web/" "$web_root/"
  sudo rsync -a --delete "$backup_dir/admin/" "$admin_root/"
  sudo systemctl reload nginx
}
trap rollback_static ERR

sudo tar -xzf "$remote_root/web.tar.gz" -C "$web_stage"
sudo tar -xzf "$remote_root/admin.tar.gz" -C "$admin_stage"
test -f "$web_stage/index.html"
test -f "$admin_stage/index.html"

sudo rsync -a --delete "$web_stage/" "$web_root/"
sudo rsync -a --delete "$admin_stage/" "$admin_root/"
sudo chown -R www-data:www-data "$web_root" "$admin_root"
sudo find "$web_root" "$admin_root" -type d -exec chmod 755 {} +
sudo find "$web_root" "$admin_root" -type f -exec chmod 644 {} +
sudo nginx -t
sudo systemctl reload nginx
trap - ERR
sudo rm -rf "$remote_root"
REMOTE_STATIC

echo "[7/7] Verify production"
curl --fail --silent --show-error --max-time 15 --output /dev/null https://xw-company.com/
curl --fail --silent --show-error --max-time 15 --output /dev/null https://xw-company.com/admin/
curl --fail --silent --show-error --max-time 15 --output /dev/null https://xw-company.com/api/site-settings

echo "Deployment complete: https://xw-company.com/"
echo "Admin: https://xw-company.com/admin/"
echo "Backup: $BACKUP_ROOT/$RELEASE_ID"
