#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SSH_HOST="${XW_SSH_HOST:-xw-lightsail}"
WEB_ROOT="${XW_WEB_ROOT:-/var/www/xw-web}"
BACKUP_ROOT="${XW_BACKUP_ROOT:-/var/backups/xw-web}"
RELEASE_ID="$(date +%Y%m%d-%H%M%S)"
REMOTE_ARCHIVE="/home/ubuntu/xw-web-${RELEASE_ID}.tar.gz"
REMOTE_STAGE="/home/ubuntu/xw-web-${RELEASE_ID}"
LOCAL_ARCHIVE="$(mktemp "${TMPDIR:-/tmp}/xw-web-${RELEASE_ID}.XXXXXX.tar.gz")"

cleanup() {
  rm -f "$LOCAL_ARCHIVE"
}
trap cleanup EXIT

if [[ -x /usr/local/opt/node@22/bin/node ]]; then
  export PATH="/usr/local/opt/node@22/bin:$PATH"
fi

cd "$PROJECT_DIR"

echo "[1/5] Build web frontend"
npm run build

echo "[2/5] Package dist"
COPYFILE_DISABLE=1 tar -czf "$LOCAL_ARCHIVE" -C dist .

echo "[3/5] Upload release"
scp "$LOCAL_ARCHIVE" "${SSH_HOST}:${REMOTE_ARCHIVE}"

echo "[4/5] Backup and publish"
ssh "$SSH_HOST" bash -s -- "$RELEASE_ID" "$REMOTE_ARCHIVE" "$REMOTE_STAGE" "$WEB_ROOT" "$BACKUP_ROOT" <<'REMOTE'
set -Eeuo pipefail

release_id="$1"
archive="$2"
stage="$3"
web_root="$4"
backup_root="$5"

sudo mkdir -p "$backup_root/$release_id" "$stage"
sudo rsync -a "$web_root/" "$backup_root/$release_id/"
sudo tar -xzf "$archive" -C "$stage"
test -f "$stage/index.html"
sudo rsync -a --delete "$stage/" "$web_root/"
sudo chown -R www-data:www-data "$web_root"
sudo find "$web_root" -type d -exec chmod 755 {} +
sudo find "$web_root" -type f -exec chmod 644 {} +
sudo nginx -t
sudo systemctl reload nginx
rm -f "$archive"
sudo rm -rf "$stage"
REMOTE

echo "[5/5] Verify production"
curl --fail --silent --show-error --max-time 15 \
  --output /dev/null https://xw-company.com/

echo "Deployment complete: https://xw-company.com/"
echo "Backup: ${BACKUP_ROOT}/${RELEASE_ID}"
