# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/) and this project adheres to SemVer.

## [0.3.0] - 2026-11-21

### Added
- 单区块占领：新增 `ChunkCoords`（含负坐标 floorDiv 换算与方块→区块换算）、`TerritoryClaimService`（以单个区块为最小单位的占领/转让/放弃、幂等、region 版本递增、变更事件）。
- 协议新增 `CHUNK_CLAIM (0x22)` 单区块占领消息与编解码；客户端 `ChunkClaimStore` 按区块应用增量并按版本丢弃乱序旧包。
- 服务端 `ClaimService` 编排领域变更→协议包→异步落库；`/nation claim|release|info` 作用于玩家脚下区块。
- 新增 5+8+4+5 项单元测试覆盖坐标换算、占领服务、协议往返与客户端视图。

### Fixed
- `ChunkBitmap.markExplored` 不再把探索时间误写进占领时间；探索时间与占领时间彻底分离，`release` 只清占领不动探索标记。

### Changed
- 服务端/客户端仍是各自单一 JAR；本次为行为更新，直接覆盖 JAR 即可升级。

## [0.2.0] - 2026-11-20

### Added
- 国家名称注册合规校验：现实国家/地区名称（中英文）不可注册，提示“为符合相关国家的法律法规”；虚构名称可注册。新增错误码 `NAME_RESTRICTED (3007)` 与 `/nation create <名称>` 命令。
- 单元测试覆盖名称校验（大小写/空格变体、虚构名、长度边界）。

### Changed
- 服务端整合为单个 fat JAR `nation-realm.jar`（含 purpur-plugin + nation-core + common-protocol + ai-worker + Jackson），更新时只替换一个文件。客户端保持单个模组 JAR。

## [0.1.0] - 2026-11-17

### Added
- 多模块 Gradle 工程（common-protocol / nation-core / ai-worker / purpur-plugin / fabric-client）。
- 版本化二进制协议与编解码、兼容矩阵基础测试。
- MySQL 8 基线迁移（国家/成员/官职/领土/建筑/经济/军队/NPC/外交/Outbox/幂等）。
- 复式账本（守恒 + 幂等）、定点金额、稀疏领土位图、GDP 只读汇总。
- Purpur 插件生命周期、配置热校验、区块探索去重异步落库、能力检测降级。
- NPC 确定性状态机、AI 意图白名单、预算护栏、熔断、DeepSeek 兼容客户端（密钥隔离）。
- Fabric 客户端网络处理、地图数据模型、三色/四色与色盲模式着色。
- CI / Nightly / Release / database / compatibility 工作流，Dependabot，CodeQL，Issue/PR 模板。
- Docker Compose（本地 MySQL + Prometheus + Grafana）。
