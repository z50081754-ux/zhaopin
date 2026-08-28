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

## SakuraPay 调研接入

本地联调时，后端必须按来源逐一放行 SakuraPay 与 TX 管理后台，不能使用通配符：

```bash
FRONTEND_ORIGINS=http://127.0.0.1:4173,http://127.0.0.1:4174
```

生产环境将上面的两个值替换为实际的、精确的 HTTPS origin（只包含协议、主机和
可选端口，不含路径或末尾 `/`），其中必须包含 SakuraPay 的精确 origin。不要用
`*` 或宽泛的子域匹配代替。若 SakuraPay 由自己的 Nginx 提供，优先把 `/api/`
反向代理到 TX API，以保持浏览器同源访问：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

同源反向代理构建 SakuraPay 时将 `VITE_API_BASE_URL` 留空；确需分域时再指定 TX API
的 HTTPS origin，例如：

```bash
VITE_API_BASE_URL=https://api.example.com npm run build
```

分域值也必须对应 `FRONTEND_ORIGINS` 中允许的 SakuraPay/管理后台精确来源。上线前
由产品负责人和隐私负责人审核活动规则与隐私说明。本发布只提供
`web3钱包产品调研` 数据收集和授权管理功能，不提供抽取、公布结果、奖励发放、
钱包转账或任何链上操作。

## 生产目录更新

```bash
sudo mkdir -p /var/www/xw-web /var/www/xw-admin
sudo rsync -a --delete dist/ /var/www/xw-web/
sudo rsync -a --delete admin/dist/ /var/www/xw-admin/
sudo nginx -t
sudo systemctl reload nginx
```
