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

## web3钱包产品调研

调研接口默认关闭。只在完成数据库迁移、管理权限和数据保留策略确认后设置
`RESEARCH_ENABLED=true`。钱包加密密钥、钱包去重 HMAC 密钥和隐私限频 HMAC 密钥
必须彼此独立；可以分别生成 32 字节随机值：

```bash
openssl rand -base64 32 # RESEARCH_WALLET_ENCRYPTION_KEY（Base64，解码后必须为 32 字节）
openssl rand -base64 32 # RESEARCH_WALLET_HASH_KEY
openssl rand -base64 32 # RESEARCH_PRIVACY_HASH_KEY
```

将三次输出分别写入部署环境的密钥存储，不要复用、打印到应用日志或提交到仓库。
本地联调示例（示例值必须替换为本次生成的临时值）：

```bash
SERVER_PORT=8081 \
RESEARCH_ENABLED=true \
RESEARCH_WALLET_ENCRYPTION_KEY='<32-byte-base64-key>' \
RESEARCH_WALLET_HASH_KEY='<independent-32-byte-hmac-key>' \
RESEARCH_PRIVACY_HASH_KEY='<independent-32-byte-hmac-key>' \
ADMIN_ACCOUNT=admin \
ADMIN_PASSWORD='<local-test-password>' \
FRONTEND_ORIGINS=http://127.0.0.1:4173,http://127.0.0.1:4174 \
mvn spring-boot:run
```

上线前，产品负责人和隐私负责人必须审核 SakuraPay 展示的活动规则、隐私说明、
数据保留期限及删除流程。本版本只收集 `web3钱包产品调研` 问卷：不包含抽取、
结果公布、奖励发放、钱包转账或任何链上操作。
