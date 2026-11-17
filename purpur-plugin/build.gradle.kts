plugins {
    `java-library`
}

// Purpur 1.21.1 插件：使用 Purpur API（Paper/Spigot/Bukkit 兼容 API）。
// 不修改 Purpur 核心；专有能力做能力检测并降级。
dependencies {
    compileOnly(libs.purpur.api)
    testImplementation(libs.purpur.api)

    api(project(":common-protocol"))
    api(project(":nation-core"))

    testImplementation(libs.bundles.test)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// 只产出插件 JAR，不打包任何服务端核心。
tasks.jar {
    archiveBaseName.set("nation-realm-purpur")
    archiveVersion.set(project.version.toString())
}
