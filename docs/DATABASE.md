# 数据库（MySQL 8）

## 约定
- InnoDB / utf8mb4 / utf8mb4_unicode_ci。
- 金额 `BIGINT` 最小货币单位（分），不用浮点。
- 时间 UTC：`BIGINT` 毫秒 epoch 或 `TIMESTAMP`。
- 所有列表分页；大字段与热表分离。
- 连接：池化、健康检查、超时、退避重试；禁止同步主线程查询。
- 领域层不直接绑定 JDBC，通过仓储接口隔离。

## 版本与并发字段
每张业务表含：`version`（乐观锁）、`server_id`、`last_write_server`。
幂等：`idempotency_keys` / `journal_entries.idempotency_key` / `construction_orders.idempotency_key`。
事件：`outbox_events.seq` 作为事件序号，发布标记与聚合版本。

## 迁移
- 脚本在 `database/mysql/migrations/`，命名 `V<n>__<desc>.sql`，记录到 `schema_migrations`。
- 发布前：检查版本 → 备份 → 迁移 → 验证 → 失败回滚；不自动删列或删数据。
- 集成测试用 Testcontainers（`@Tag("integration")`）：约束、事务、并发版本冲突、回滚。

## 并发控制
- 两事务争抢同一国家版本时只有一个成功（乐观锁 `version` CAS）。
- 重复请求不重复扣款（幂等键唯一索引）。
- 非法转账（借贷不平衡）在领域层与 DB 双校验。

## 契约
- `journal_lines`：正=借、负=贷，借贷必相等；外键到 `journal_entries`。
- `npcs.ai_visible` 默认 TRUE，永久 AI 标记，禁止隐藏。
- `law_versions.structured_rules` 为 JSON 结构化规则，禁止自由文本直接变代码。
