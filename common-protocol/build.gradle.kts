plugins {
    `java-library`
}

// common-protocol 是纯 Java 模块：只放可共享的纯 Java 类型，
// 禁止引入 Minecraft / Paper / Fabric 依赖（ArchUnit 规则会在 CI 校验）。
dependencies {
    testImplementation(libs.bundles.test)
}

java {
    withSourcesJar()
}
