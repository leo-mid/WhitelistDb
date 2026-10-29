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

include(
        "common",
        "fabric",
        "fabric-1.21",
        "neoforge",
        "neoforge-1.21",
        "paper",
        "paper-1.21"
)
