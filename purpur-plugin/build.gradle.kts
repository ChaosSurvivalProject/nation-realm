plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

// Purpur 1.21.1 插件：使用 Purpur API（Paper/Spigot/Bukkit 兼容 API）。
// 不修改 Purpur 核心；专有能力做能力检测并降级。
dependencies {
    compileOnly(libs.purpur.api)
    testImplementation(libs.purpur.api)

    // 将服务端全部模块编入同一个插件 JAR，方便更新（只替换一个文件）。
    implementation(project(":common-protocol"))
    implementation(project(":nation-core"))
    implementation(project(":ai-worker"))

    testImplementation(libs.bundles.test)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// 单一服务端制品：fat JAR，包含领域层、协议与 AI 工作器；不打包任何服务端核心。
// 产物名固定为 nation-realm.jar，便于更新时直接覆盖 plugins/ 下同名文件。
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("nation-realm")
    archiveClassifier.set("")
    archiveVersion.set("") // 文件名不含版本号：nation-realm.jar
    mergeServiceFiles()
    // 不使用 relocate：Shadow 8.1.1 的 ASM 不支持 Java 21 字节码（major 65）。
    // Purpur 服务端默认不在插件类加载顶层提供 Jackson，冲突风险低。
    // 排除签名文件，防止 JAR 签名校验失败。
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// build 时产出单一服务端 JAR；普通薄 JAR 标记为 thin 以避免混淆。
tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
