# XW 招聘前台部署说明

## 环境要求

- Node.js 22.13 或更高版本
- Nginx 或其他支持 SPA 回退的静态 Web 服务

## 构建

1. 将 `.env.production.example` 复制为 `.env.production`。
2. 把 `VITE_API_BASE_URL` 改为 XW 提供的正式 HTTPS API 地址。
3. 执行 `npm install`。
4. 执行 `npm run build`。
5. 将生成的 `dist` 目录内容部署到网站根目录。

## Nginx SPA 配置

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

不要把 `admin`、`backend`、数据库文件或服务器密钥部署到前台服务器。
