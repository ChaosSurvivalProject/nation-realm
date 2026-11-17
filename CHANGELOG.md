# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/) and this project adheres to SemVer.

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
