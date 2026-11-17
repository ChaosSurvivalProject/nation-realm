# 管理员指南

## 命令
- `/nation` 主命令；`/nation admin` 管理；`/nation map` 地图；`/nation npc` NPC。
- 权限节点：`nationrealm.use` / `nationrealm.admin` / `nationrealm.map` / `nationrealm.npc`。

## 配置
- `config.yml`；凭据用环境变量 `NATION_DB_HOST/USER/PASS`、`NATION_AI_KEY`。
- 配置非法时插件安全关闭，日志给出原因，不带病运行。

## 建都闭环（首个可执行切片）
Purpur 启动 → MySQL 迁移 → 玩家建城 → 区块探索落库 → 服务端授权真人官员 → Fabric 登录握手 → 请求区域快照 → 显示首都/已探索/未知区块 → 建造小型物资中心 → 双录扣款与施工 → 60 秒差分刷新 → 断线重连。

## 破坏性操作
确认 + 审计（操作者/旧值/新值/原因/时间/关联交易）。监狱、战争、税收提供申诉与管理员审计工具，防滥用与恶意封禁。

## 备份与恢复
发布前备份 → 迁移 → 验证 → 失败回滚；定期恢复演练。不自动删列或删数据。
