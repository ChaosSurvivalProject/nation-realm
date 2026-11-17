rootProject.name = "nation-realm"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://repo.purpurmc.org/snapshots") {
            name = "purpur"
            mavenContent { snapshotsOnly() }
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
            // 该仓库同时承载 release 传递依赖（brigadier、bungeecord-chat）与 Paper 快照。
        }
        maven("https://libraries.minecraft.net/") {
            name = "minecraft-libraries"
            mavenContent { releasesOnly() }
        }
    }
}

include("common-protocol")
include("nation-core")
include("ai-worker")
include("purpur-plugin")
// fabric-client 使用 Fabric Loom，作为独立构建（见 fabric-client/settings.gradle.kts），
// 避免 Loom 的 Minecraft 依赖污染服务端公共构建。
