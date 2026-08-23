# XW Recruitment API

Java 21 + Spring Boot 3.5 后端，向 Vue 3 招聘官网提供候选人投递和招聘管理接口。

## 分层

- `application`：候选人投递业务、实体、数据访问与接口
- `admin`：管理员登录、候选人查询、阶段修改与简历下载
- `storage`：简历文件存储抽象及本地开发实现
- `config`：Spring Security、CORS 和统一异常处理
- `db/migration`：Flyway 数据库版本管理

## 本地运行

```bash
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
mvn spring-boot:run
```

默认使用本地 H2 文件数据库，API 地址为 `http://localhost:8080`。Vue 开发服务器通过 Vite 代理访问 `/api`。

正式环境必须设置 `.env.example` 中的数据库、管理员密码、前端域名等变量，不能使用默认密码。
