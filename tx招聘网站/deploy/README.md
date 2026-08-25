# XW 招聘系统部署结构

- `/opt/xw`：项目源码和 Spring Boot 构建产物。
- `/var/www/xw-web`：招聘官网 `dist/` 的生产构建文件，可单独交付给外部服务器。
- `/var/www/xw-admin`：管理后台 `admin/dist/` 的生产构建文件，只部署在自有服务器。
- `/etc/xw-recruitment.env`：数据库、管理员账号等敏感环境变量，不提交到代码仓库。
- `xw-recruitment.service`：通过 systemd 管理 Java 进程。
- `nginx-xw.conf`：分别提供官网与 `/admin/` 后台，并将 `/api/` 反向代理到本机 `8080` 端口。

公网只开放 Nginx 的 80/443 端口。Spring Boot 与 PostgreSQL 只监听服务器本机，不直接暴露到互联网。

## 前端项目与构建

- 官网源码位于项目根目录的 `src/`，执行 `npm run build`，输出到 `dist/`。
- 后台源码位于 `admin/`，执行 `npm run build:admin`，输出到 `admin/dist/`。
- 同时构建两个项目：`npm run build:all`。
- 后台本地开发：`npm run dev:admin`，默认地址为 `http://localhost:3001/admin/`。

后台的环境变量见 `admin/.env.example`。后台和 API 同源部署时，`VITE_API_BASE_URL` 保持为空；若以后分域部署，则填入 API 的 HTTPS 地址，并在 Spring Boot 中放行后台来源域名。

## 生产目录更新

```bash
sudo mkdir -p /var/www/xw-web /var/www/xw-admin
sudo rsync -a --delete dist/ /var/www/xw-web/
sudo rsync -a --delete admin/dist/ /var/www/xw-admin/
sudo nginx -t
sudo systemctl reload nginx
```
