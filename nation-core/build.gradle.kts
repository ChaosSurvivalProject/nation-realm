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
}

tasks.withType<Test>().configureEach {
    // 数据库集成测试需要 Docker，单独用 @Tag("integration") 标记，CI 的 database job 才运行。
    useJUnitPlatform { excludeTags("integration") }
}
