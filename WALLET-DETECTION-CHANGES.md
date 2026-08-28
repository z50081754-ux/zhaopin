# iOS 钱包检测接入说明

## 已接入位置
- 前台有效浏览：`src/composables/useVisitTracking.ts`
- iOS 钱包检测：`src/utils/iosWalletDetection.ts`
- 数据库存储：`website_visits.detected_wallets`
- Flyway 升级：`backend/src/main/resources/db/migration/V12__add_detected_wallets_to_website_visits.sql`
- 后台有效浏览列表：新增“检测到的钱包”列

## 当前目标钱包
Bitpie、Trust Wallet、Solflare、MetaMask、Ronin Wallet、Phantom、Exodus、Bitget Wallet、imToken。

## 触发规则
仍沿用原站规则：页面可见停留满 15 秒后才形成有效浏览。在有效浏览首次达标时进行一次被动 iOS 钱包环境检测，并与有效浏览记录一起提交。

## 隐私/权限边界
检测代码不调用账户连接，不调用 `eth_requestAccounts`，不读取钱包地址、余额、助记词、私钥，不请求签名或交易。

## iOS 限制
普通 Safari 无法可靠扫描 iPhone 已安装 App。因此本字段只记录网页实际检测到的钱包 Provider、JS 对象、EIP-6963 信息或钱包内置浏览器特征。空值表示“当前网页环境未确认检测到”，不能解释为“手机肯定没有安装钱包”。
