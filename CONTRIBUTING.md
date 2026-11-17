# 贡献指南

## 前置
- JDK 21、Gradle Wrapper（仓库已含）。
- 阅读 `AGENTS.md`（版本边界、模块规则、禁止事项）。

## 流程
1. Fork / 建分支：`feat/xxx`、`fix/xxx`。
2. 先写失败测试，再实现；小步提交。
3. 本地验证：
   ```bash
   ./gradlew spotlessApply build --no-daemon
   ```
4. 提交信息用 Conventional Commits。
5. 开 PR，填写模板，关联 Issue。CI 必须全绿。

## 测试分层
- 单元测试：无需 Docker，`./gradlew test`。
- 集成测试：`@Tag("integration")`，需要 Docker（Testcontainers MySQL）。

## 禁止
见 `AGENTS.md` 第 6 节与 `SECURITY.md`。
