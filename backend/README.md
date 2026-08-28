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

调研报名入口默认关闭。`RESEARCH_ENABLED` 只是公开提交的 kill switch；只在完成数据库
迁移、管理权限和数据保留策略确认后设置为 `true`。关闭入口后，若数据库已有加密调研
记录，仍须提供原来的三项密钥，ADMIN 才能读取、导出和管理这些记录。未提供密钥时应用
可以保持入口关闭并启动，但 ADMIN 会明确报告加密数据不可用；只提供部分密钥会拒绝启动。

钱包加密密钥、钱包去重 HMAC 密钥和隐私限频 HMAC 密钥必须完整且彼此独立，也不能把
加密密钥的配置值复用为 HMAC 密钥。加密密钥解码后必须为 32 字节，两项 HMAC 值均须
至少 32 个 UTF-8 字节。可以分别生成随机值：

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
ADMIN_FRONTEND_ORIGINS=http://127.0.0.1:3001 \
RESEARCH_TRUSTED_PROXIES=127.0.0.1,::1 \
mvn spring-boot:run
```

`FRONTEND_ORIGINS` 和 `ADMIN_FRONTEND_ORIGINS` 都只接受逐一列出的精确 `http`/`https`
origin（协议、主机和可选端口），不能包含路径、末尾 `/` 或通配符。前者用于公开调研和
既有站点 API，后者用于带凭据的 ADMIN API；同源部署可将对应列表留空。公开调研 CORS
不发送凭据，ADMIN 和既有站点 API 保留凭据。

限频默认只按直连 TCP 客户端地址识别，不信任浏览器发送的 `X-Forwarded-For`、
`CF-Connecting-IP` 或 `X-Real-IP`。只有后端确实位于自管代理之后时，才把每一跳代理的
精确 IP 写入 `RESEARCH_TRUSTED_PROXIES`。边缘代理必须丢弃客户端传入的转发链并重写
`X-Forwarded-For`；仓库 nginx 配置在同机代理场景使用 `$remote_addr` 覆盖该头，因此
后端部署时可显式配置 `127.0.0.1,::1`。多级代理必须逐跳覆盖/追加受控地址，并将所有
受信任代理的精确 IP 列入配置，不能配置网段或通配符。

上线前，产品负责人和隐私负责人必须审核 SakuraPay 展示的活动规则、隐私说明、
数据保留期限及删除流程。本版本只收集 `web3钱包产品调研` 问卷：不包含抽取、
结果公布、奖励发放、钱包转账或任何链上操作。
