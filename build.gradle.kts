plugins {
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.chaos.nation"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    // 仅对纯 Java 模块统一应用基础配置；fabric-client 为独立构建，不在此列。
    if (name != "fabric-client") {
        apply(plugin = "java-library")
        apply(plugin = "com.diffplug.spotless")

        extensions.configure<JavaPluginExtension> {
            toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
        }

        // 仓库统一在 settings.gradle.kts 的 dependencyResolutionManagement 声明。

        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                target("src/**/*.java")
                googleJavaFormat()
                removeUnusedImports()
                trimTrailingWhitespace()
                endWithNewline()
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = false
            }
        }
    }
}
