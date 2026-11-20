plugins {
    // Loom 1.7.x 兼容 Gradle 8.10；如需 Loom 1.10+ 请将 Gradle Wrapper 升级至 8.12+。
    id("fabric-loom") version "1.7-SNAPSHOT"
    java
}

// Fabric 1.21.1 客户端模组：仅客户端侧。
// 不要求其他玩家安装；客户端无国家规则决定权，所有操作由服务端验证。
version = "0.1.0"
group = "com.chaos.nation"

repositories { mavenCentral() }

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.114.0+1.21.1")

    // 复用纯 Java 协议模块（本地构建产物，避免发布期私服）
    implementation(files("../common-protocol/build/libs/common-protocol-0.1.0-SNAPSHOT.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.jar {
    archiveBaseName.set("nation-realm-fabric")
}
