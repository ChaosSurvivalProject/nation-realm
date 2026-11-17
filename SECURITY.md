# 安全策略

## 报告漏洞
请勿通过公开 Issue 报告安全漏洞。使用 GitHub 私有漏洞报告（Security → Report a vulnerability）。

## 凭据处理（硬性要求）
- **API Key**：AI 工作器只从环境变量 `NATION_AI_KEY` 读取，绝不写入仓库、`config.yml` 示例、MySQL 明文字段、客户端包或玩家日志。默认模型配置键 `deepseek-flash-4.1` 只是配置名，不含密钥。
- **数据库**：最小权限账号；凭据通过 `NATION_DB_USER` / `NATION_DB_PASS` 注入；禁止客户端直连 MySQL。
- **发布令牌**：仅存于 GitHub Repository Secrets；fork 上的敏感发布任务不得运行。

## 已实现的防护
- 客户端封包：长度 / 枚举 / 范围 / 权限 / 状态校验；未知字段跳过但非法值拒绝。
- AI 意图：白名单 action、forbidden 字段检测、JSON Schema 校验、服务端状态机二次验证。
- NPC 身份：永久 AI 标记（`ai_visible`），禁止不可见/同形字符冒名。
- 经济：复式记账守恒、幂等键防重复扣款、定点整数金额。
- 权限：官职只授真人；NPC 无政治权力；授权带期限。

## 支持版本
仅 1.21.1 / Java 21。其他版本不在支持范围。
