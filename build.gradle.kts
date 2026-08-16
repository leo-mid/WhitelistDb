plugins {
    java
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
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
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(25)
        }
    }
}

//project(":fabric-1.21") {
//    plugins.withId("java") {
//        extensions.configure<JavaPluginExtension> {
//            toolchain {
//                languageVersion.set(JavaLanguageVersion.of(21))
//            }
//        }
//
//        tasks.withType<JavaCompile>().configureEach {
//            options.release.set(21)
//        }
//    }
//}