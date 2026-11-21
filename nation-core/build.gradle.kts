plugins {
    `java-library`
}

// nation-core：领域层，纯 Java，无 Minecraft / JDBC 直接依赖。
// 仓储以接口形式定义，便于未来接入缓存与跨服，且可在无 MC 环境下测试。
dependencies {
    api(project(":common-protocol"))

    testImplementation(libs.bundles.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mysql)
    // 集成测试通过 CI 的 MySQL service 直连校验迁移；驱动仅测试期可见，不进生产依赖。
    testRuntimeOnly(libs.mysql.connector)
}

// 单元测试：无需 Docker，排除 integration 标签。
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("integration") }
}

// 集成测试：需 Docker/MySQL，CI 的 database job 单独运行（含 @Tag("integration") 的迁移校验）。
val integrationTest by tasks.registering(Test::class) {
    description = "Runs @Tag(integration) tests against a real MySQL (requires Docker)."
    group = "verification"
    useJUnitPlatform { includeTags("integration") }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
}
