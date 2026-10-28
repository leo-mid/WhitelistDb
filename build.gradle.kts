plugins {
    java
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.140" apply false
}

allprojects {
    group = property("maven_group") as String
    version = property("mod_version") as String

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.nucleoid.xyz/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    // Minecraft 26.x runs on Java 25, Minecraft 1.21.x runs on Java 21.
    // :common is bundled into both, so it targets Java 21 bytecode (which also runs fine on Java 25).
    val toolchainVersion = if (name == "fabric-1.21") 21 else 25
    val releaseVersion = if (name == "fabric-1.21" || name == "common") 21 else 25

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(toolchainVersion))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(releaseVersion)
        }
    }
}
