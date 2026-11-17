# 实施状态（诚实报告）

生成日期：2026-11-17。标注规则：只有“已运行且通过”才算完成。

## 已编译并通过自动测试
- `common-protocol`：`./gradlew :common-protocol:test` —— 协议编解码往返、握手/领土增量/建筑/军队/NPC 包、未知 ID 跳过、截断拒绝、版本兼容。**已运行，通过。**
- `nation-core`：`./gradlew :nation-core:test` —— 复式账本守恒、幂等、定点金额、稀疏位图、外交状态机、NPC 状态机。**已运行，通过。**
- `ai-worker`：`./gradlew :ai-worker:test` —— 意图白名单、forbidden 字段、预算护栏、熔断。**已运行，通过（修复 Jackson 枚举大小写后）。**
- `purpur-plugin`：`./gradlew :purpur-plugin:test` —— 探索去重与批次。**已运行，通过。**

## 已编译但未做玩法验证
- Fabric 客户端：`./gradlew -p fabric-client build` **已运行，BUILD SUCCESSFUL**，产出 `nation-realm-fabric-0.1.0.jar`（Loom 1.7.4，兼容 Gradle 8.10.2；已下载 Minecraft 1.21.1 映射并 remap）。未做游戏内实际渲染验证。
- 数据库集成测试（Testcontainers / `@Tag("integration")`）：**未运行**（本环境无 Docker；CI 的 database job 会运行）。迁移 SQL 已备。
- 完整 Purpur 服务器启动闭环（建都→探索→地图→施工→重连）：**未运行**（需要真实 Purpur 1.21.1 服务器与 MySQL）。领域逻辑与插件骨架已备。

## 明确未完成（禁止标成完成）
- 阶段 4：Fabric 完整 3D 全息渲染、50 万区块性能调优、小地图/世界图全部 GUI 控件。
- 阶段 5：建筑模板数据文件、路径/到达时间模型、战斗结算接口、法规投票全流程——领域基础已备，服务端命令与持久化 wiring 待补。
- 阶段 7：Velocity 子模块、Redis 集成、多服故障演练。
- 阶段 8：Hangar/Modrinth 实际发布（需 secrets）、数据库备份恢复演练脚本自动化。
- 可观测性：Micrometer 指标端点与结构化日志落地（配置与面板已备，代码埋点待补）。

## 编译环境说明
- 本环境初始为 Java 20，已安装 JDK 21（`/opt/jdk21`）用于构建。
- Gradle Wrapper 8.10.2；`gradle-wrapper.properties` 使用官方 `services.gradle.org` URL。
- Fabric 客户端使用 Loom 1.7.4（兼容 Gradle 8.10.2）；若升级 Loom 至 1.10+ 需同步把 Wrapper 升至 8.12+。
