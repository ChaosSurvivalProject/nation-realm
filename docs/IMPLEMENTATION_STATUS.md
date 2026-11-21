# 实施状态（诚实报告）

生成日期：2026-11-17，更新 2026-11-22。标注规则：只有“已运行且通过”才算完成。

## 本次更新（0.4.0）—— 电台 + 近场语音一期（仅控制面，音频栈未实现）
按已定方案**只做一期**；二期（voice-api/voice-server/客户端音频栈/近场与己方台/调谐 UI/2D 覆盖图）与三期（跨服/中继/干扰俘获/3D overlay）**未开始，不得标完成**。

### 一期已实现且已运行通过
- **CI 修绿**：`V1__baseline.sql` currency 字段 `VARCHAR(8)→VARCHAR(16)`（`'CNY_MINOR'` 9 字符导致 MySQL 8 `ERROR 1067` 迁移中止，Database Integration 失败根因）；`release.yml` 步骤 `if` 禁止直接引用 `secrets` 上下文（0 jobs failure），改为 job 级 env 中转。本地 `./gradlew test` 通过、SQL 默认值静态校验全部 fit；**远端 Database job 结果待下一次推送后观测**。
- **band plan 与领域对象**：`Frequency`(kHz 整数/MHz 显示)、`BandPlan`(3 预设频段/频道栅格/亚音)、呼号禁用词校验(现实国家/紧急呼号)、`Station/Transmitter/RadioSubscription/TransmissionSession/SignalReport`、仓储接口、`RelayLink/JammingRecord` 三期骨架。13 项单测通过。
- **权限矩阵增量 + `RadioAccessPolicy`**：5 个 RADIO_* 权限位复用现有矩阵；单一纯函数判定 PUBLIC 恒放行/FACTION 同国/盟国可听不可发/中立交战不可听/NPC 永不发射建塔。7 项单测通过。
- **`LinkBudget` 纯函数**：标准 FSPL + 游戏化损耗，参数集中且标"需实测标定"，确定性慢衰落；只算电台链路、与近场语音距离严格分离；超出设计半径静默；信号四档。6 项单测（含 200 种子确定性属性测试）通过。
- **Radio* 协议族**：8 消息(0x40-0x47)、`RADIO_*` 错误码段(5000-5009)、握手 `Flags.RADIO(1<<4)`、协议次版本 `0→1`、主版本仍 1；尾部字段+length 前缀+未知字段跳过+非法包拒绝。9 项单测通过。音频帧不进协议。
- **MySQL 迁移**：`V2__radio.sql`（电台表含主键/索引/FK/幂等键/版本，对齐 Outbox 与 V1 版本表），静态校验 FK 目标存在、默认值 fit；**未在真实 MySQL 执行（本环境无 Docker/MySQL）**。
- **配置骨架 + 进服提示**：`RadioSettings` 中 `clientDownloadUrl` 默认空=**待运营补齐**、拒绝占位链接、链路参数标需实测标定、`crossServer=false`；进服一次性可点击提示(Adventure ClickEvent.openUrl)+`/radio` 复述带冷却，文案写明"可选安装/不影响进服"，无链接只发文案不编造 URL，不踢出/不锁入口。2 项提示单测通过。
- **全量**：`./gradlew spotlessCheck build` **BUILD SUCCESSFUL**，单一服务端 fat JAR `nation-realm.jar` 重新产出（约 2.67MB）。

### 一期未做 / 诚实边界
- 音频栈全部未实现：voice-api 票据与 HMAC、voice-server UDP/Opus/扇出/限流/验签、客户端麦克风/抖动缓冲/播放/声像/调谐 UI —— 均为二期。
- 近场语音（说话玩家↔周围玩家）距离/朝向/遮挡模型未实现；己方台 FACTION/ROLE·SQUAD/SELF 的实际收发未接通。
- 真实的调谐/订阅/发射协议处理器、塔覆盖与领土位图求交、易主重算、跨服与中继、干扰俘获、3D overlay 均未实现。
- **下载链接无落点**：仓库 0 releases，`clientDownloadUrl` 留空，提示只发文案；需运营先建 Releases/资源中心下载项（带版本兼容说明与 SHA-256）再填真实 URL。
- `NATION_VOICE_SECRET` 环境变量一期未使用（二期票据验签才引入）；`radio.broadcast` AI 播报意图未实现（三期）。
- 所有链路预算参数（半径/损耗/功率/信纳比阈值）均为游戏化建议值，**需实测标定**，非已验证数字。

## 上次更新（0.3.0）—— 单区块占领优化
- **占领最小单位 = 单个区块**：新增 `ChunkCoords`（负坐标 floorDiv/floorMod、方块→区块换算）、`TerritoryClaimService`（单区块占领/转让/放弃、幂等、region 版本递增、变更事件），**单元测试已运行，通过**（坐标 4 项 + 服务 8 项）。
- **协议 `CHUNK_CLAIM (0x22)`**：单区块占领消息与编解码，**往返测试已运行，通过**（含负坐标与放弃包）；`PacketCodec` 全量测试通过。
- **服务端 `ClaimService`**：领域变更→协议包→异步落库编排；`/nation claim|release|info` 作用于玩家脚下区块。**单元测试已运行，通过**（4 项）。
- **客户端 `ChunkClaimStore`**：按区块应用增量、按版本丢弃乱序旧包，防地图回退。**单元测试已运行，通过**（5 项，Fabric 独立构建）。
- **修复**：`ChunkBitmap.markExplored` 不再把探索时间写成占领时间；探索/占领时间分离，`release` 只清占领。**测试已运行，通过。**
- 服务端/客户端 JAR 已重新构建产出（`nation-realm.jar` 与 `nation-realm-fabric-0.1.0.jar`），fat JAR 已核验含全部新类。

## 上次更新（0.2.0）
- **服务端整合为单一 JAR**：`./gradlew build` 产出 `purpur-plugin/build/libs/nation-realm.jar`（fat JAR，含 purpur-plugin + nation-core + common-protocol + ai-worker + Jackson，1352 个条目，约 2.5MB）。**已运行，BUILD SUCCESSFUL，已核验内含全部模块类。**
- **国家名称注册合规校验**：`NationNameRegistry` 拦截现实国家/地区名称（中英文，含大小写/空格变体），返回 `NAME_RESTRICTED(3007)` 与“为符合相关国家的法律法规，该名称不可注册”；虚构名称放行。`/nation create <名称>` 已接入。**单元测试已运行，通过。**
- 更新时只需替换 `nation-realm.jar`（服务端）与 `nation-realm-fabric-0.1.0.jar`（客户端）。

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
