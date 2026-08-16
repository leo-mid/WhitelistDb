pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC"
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "WhitelistDbFabric"

// Fabric 1.21 Support is still work in progress
include(
        "common",
        "fabric",
        "forge",
        "paper"
)