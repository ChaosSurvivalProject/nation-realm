plugins {
    `java-library`
    application
}

// 独立 AI 工作器：Java 21 HttpClient 调用 DeepSeek 兼容接口，
// 只返回受约束意图 JSON，绝不接触数据库凭据 / OP / 控制台。
dependencies {
    api(libs.jackson.databind)
    api(libs.jackson.dataclass)
    implementation(project(":common-protocol"))

    testImplementation(libs.bundles.test)
}

application {
    mainClass.set("com.chaos.nation.ai.AiWorkerMain")
}

// API Key 只从环境变量读取，绝不写入仓库或配置示例的明文值。
tasks.withType<JavaExec>().configureEach {
    environment("NATION_AI_KEY", System.getenv("NATION_AI_KEY") ?: "")
}
