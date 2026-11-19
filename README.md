# NationRealm（国家领土系统）

基于 **Purpur 1.21.1（Java 21）** 的大型国家、领土、政府、经济、军事、外交、NPC 与可视化地图系统。

> 服务端权威：所有国家规则、权限、成本、冷却与范围都由服务端验证；客户端只是可选的高精细地图增强，安装与否都能进服。

## 架构总览

| 模块 | 职责 | 依赖边界 |
|------|------|----------|
| `common-protocol` | 纯 Java：版本化二进制协议、消息、错误码、编解码 | 无 MC / Paper / Fabric 依赖 |
| `nation-core` | 领域层：国家/成员/官职、复式账本、领土位图、外交状态机、NPC 状态机、GDP | 纯 Java，仓储以接口隔离 |
| `ai-worker` | 独立 AI 工作器：DeepSeek 兼容接口、意图 JSON 校验、预算/熔断 | Jackson + JDK HttpClient |
| `purpur-plugin` | Purpur 插件：生命周期、命令、权限、探索落库、能力检测 | purpur-api（compileOnly） |
| `fabric-client` | Fabric 1.21.1 客户端：2D/3D 地图、色盲模式、网络处理 | 独立 Loom 构建 |

## 当前状态（诚实的进度）

本仓库按 9 个阶段推进。已实现并有自动测试覆盖的核心：

- ✅ 阶段 0：Gradle 多模块、Java 21 工具链、JUnit、Spotless、CI、许可证、模板
- ✅ 阶段 1：MySQL 8 基线迁移（全部核心表）、定点金额、复式账本、稀疏领土位图
- ✅ 阶段 2：插件生命周期、配置热校验、区块探索去重异步落库、Purpur 能力检测降级
- ✅ 阶段 3：版本化二进制协议、编解码往返、未知字段跳过、非法包拒绝、兼容测试
- ✅ 阶段 5：经济守恒、外交状态机、GDP 只读汇总、权限矩阵（NPC 无政治权）
- ✅ 阶段 6：NPC 确定性行为树、AI 意图白名单校验、预算护栏、熔断器、密钥隔离
- ✅ 阶段 7/8：CI/CD、Nightly、Release、幂等/Outbox 表结构、Docker Compose 监控
- 🟡 阶段 4：Fabric 客户端基础（网络处理、地图数据模型、着色/色盲）+ GUI 骨架已建，**完整 3D 渲染与 50 万区块性能调优仍在进行**
- 🟡 阶段 7 跨服：Outbox/幂等表与领域基础已备，Velocity 子模块为骨架，**多服集成演练待补**

> 详情见 [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)。未运行/未完成项不会被标为完成。

## 快速开始

```bash
# 需要 JDK 21 与 Gradle 8.10+（仓库含 Wrapper）
./gradlew build --no-daemon

# 仅跑服务端测试（无需 Docker；集成测试用 @Tag("integration") 隔离）
./gradlew test --no-daemon

# 数据库集成测试（需要 Docker）
./gradlew :nation-core:test -PincludeIntegration --no-daemon

# Fabric 客户端（独立构建，需要网络下载 Minecraft 映射）
./gradlew -p fabric-client build
```

## 交付物与更新（两个 JAR）

为方便更新，服务端全部模块已整合为**单个** fat JAR，客户端为**单个**模组 JAR：

| 用途 | 文件 | 部署位置 |
|------|------|----------|
| 服务端 | `purpur-plugin/build/libs/nation-realm.jar` | `plugins/nation-realm.jar`（内含领域层/协议/AI 工作器） |
| 客户端 | `fabric-client/build/libs/nation-realm-fabric-0.1.0.jar` | 玩家 `mods/`（可选安装） |

更新时**只需替换对应的一两个 JAR**，无需替换多个模块文件。服务端 JAR 文件名不含版本号，便于直接覆盖。

## 构建与运行

- 服务端核心：Purpur 1.21.1（固定构建号用于封测，见 `docs/ARCHITECTURE.md`）
- 数据库：MySQL 8，迁移脚本在 `database/mysql/migrations/`
- 凭据：一律通过环境变量注入（`NATION_DB_*`、`NATION_AI_KEY`），**绝不写入仓库**
- 国家名称注册：`/nation create <名称>`；与现实国家/地区重名的名称会被拒绝并提示“为符合相关国家的法律法规，该名称不可注册”。虚构名称可正常注册。

## 文档

- [架构](docs/ARCHITECTURE.md) · [协议](docs/PROTOCOL.md) · [数据库](docs/DATABASE.md)
- [经济](docs/ECONOMY.md) · [领土](docs/TERRITORY.md) · [NPC 与 AI](docs/NPC_AI.md)
- [管理](docs/ADMIN.md) · [玩家指南](docs/PLAYER_GUIDE.md) · [跨服](docs/CROSS_SERVER.md)

## 许可

MIT —— 详见 [LICENSE](LICENSE)。
