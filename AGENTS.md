# AGENTS.md —— 自动化 Agent 必读

所有 AI 编程 Agent 在修改本仓库前**必须**先完整阅读本文件与 `docs/ARCHITECTURE.md`。

## 1. 版本边界（不得发明或猜测）
- 服务端：**Purpur 1.21.1 / Java 21**，依赖 `org.purpurmc.purpur:purpur-api`，按仓库可解析版本配置。
- 客户端：**Fabric 1.21.1 / Java 21 / 仅客户端侧**，Yarn 映射 `1.21.1+build.3`。
- 协议主版本 1，次版本 0（见 `ProtocolConstants`）。遇到 1.21.1 API 不确定时：查证并在提交信息中写明来源，禁止凭记忆发明方法。

## 2. 命令
```bash
./gradlew build --no-daemon              # 完整服务端构建（含 spotlessCheck）
./gradlew test --no-daemon               # 单元测试（无需 Docker）
./gradlew spotlessApply --no-daemon      # 格式化
./gradlew -p fabric-client build         # 客户端（独立 Loom 构建）
```
集成测试用 `@Tag("integration")`，仅在 CI 的 database job（有 Docker）运行。

## 3. 代码风格
- Google Java Format（`spotlessApply`）；UTF-8；4 空格；无通配符 import。
- 公共 API、配置键、协议字段、数据库变更**必须**同时更新文档与迁移。

## 4. 模块规则（ArchUnit 在 CI 校验）
- `common-protocol`：纯 Java，**禁止** import `org.bukkit.*` / `net.minecraft.*` / JDBC。
- `nation-core`：领域层，**禁止** import `org.bukkit.*` / `net.minecraft.*` / `java.sql.*`。
- 服务端 API **不得**进入客户端；Fabric 适配层保持薄。
- 仓储以接口隔离，领域层不绑定 JDBC。

## 5. 测试要求
- 先写失败测试，再写最小实现。
- 核心规则、数据库迁移、协议兼容、权限、经济守恒、领土合并、NPC 状态机、跨服幂等必须有自动测试。
- 未运行的命令标注“未运行”，不得声称通过。

## 6. 禁止事项
- 不把密钥/令牌/凭据写进仓库、配置示例、日志、客户端包或 MySQL 明文字段。
- 不用浮点数存钱；不用内存 Map 代替持久化事实源。
- 不做 GUI 空壳；不留 TODO 冒充完成；不删除失败测试或降低覆盖率伪装修复。
- 不修改 Minecraft / Purpur / Fabric 内部类私有逻辑绕过稳定 API。
- 不把外部代码生成模型的输出直接当控制台命令执行。

## 7. 提交规范
Conventional Commits：`feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`, `ci:`。
小步提交；每个纵向切片都要编译、测试、可运行。
