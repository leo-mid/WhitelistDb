plugins {
    id("net.fabricmc.fabric-loom-remap")
    java
}

val minecraftVersions = listOf(
    "1.21.11",
    "1.21.10",
    "1.21.9",
    "1.21.8",
    "1.21.7",
    "1.21.6",
    "1.21.5",
    "1.21.4",
    "1.21.3",
    "1.21.2",
    "1.21.1",
    "1.21"
)

val minecraftVersion = providers.gradleProperty("minecraftVersion").orNull
    ?.takeIf { it.startsWith("1.") }
    ?.also {
        if (it !in minecraftVersions) {
            throw GradleException("Unsupported Minecraft version '$it' for :fabric-1.21. Supported: $minecraftVersions")
        }
    }
    ?: "1.21.11"

base {
    archivesName.set("${project.property("archives_base_name")}-fabric-$minecraftVersion")
}

val fabricApiVersions = mapOf(
    "1.21.11" to "0.141.4+1.21.11",
    "1.21.10" to "0.138.4+1.21.10",
    "1.21.9" to "0.134.1+1.21.9",
    "1.21.8" to "0.136.1+1.21.8",
    "1.21.7" to "0.129.0+1.21.7",
    "1.21.6" to "0.128.2+1.21.6",
    "1.21.5" to "0.128.2+1.21.5",
    "1.21.4" to "0.119.4+1.21.4",
    "1.21.3" to "0.114.1+1.21.3",
    "1.21.2" to "0.106.1+1.21.2",
    "1.21.1" to "0.116.13+1.21.1",
    "1.21" to "0.102.0+1.21"
)

val permissionApiVersions = mapOf(
    "1.21.11" to "0.6.1",
    "1.21.10" to "0.5.0",
    "1.21.9" to "0.5.0",
    "1.21.8" to "0.4.1",
    "1.21.7" to "0.4.1",
    "1.21.6" to "0.4.1",
    "1.21.5" to "0.3.3",
    "1.21.4" to "0.3.3",
    "1.21.3" to "0.3.3",
    "1.21.2" to "0.3.3",
    "1.21.1" to "0.3.3",
    "1.21" to "0.3.3"
)

val placeholderApiVersions = mapOf(
    "1.21.11" to "2.8.2+1.21.10",
    "1.21.10" to "2.8.2+1.21.10",
    "1.21.9" to "2.8.0+1.21.9",
    "1.21.8" to "2.7.2+1.21.8",
    "1.21.7" to "2.7.1+1.21.6",
    "1.21.6" to "2.7.1+1.21.6",
    "1.21.5" to "2.6.4+1.21.5",
    "1.21.4" to "2.5.2+1.21.3",
    "1.21.3" to "2.5.2+1.21.3",
    "1.21.2" to "2.5.0+1.21.2",
    "1.21.1" to "2.4.2+1.21",
    "1.21" to "2.4.2+1.21"
)

// Version-specific source folders (src/compat/<name>/java). Only the ones matching the
// selected Minecraft version are compiled, so the main code can stay identical for every 1.21.x.
//
//   profile_legacy 1.21 - 1.21.8 GameProfile is a class: getId() / getName()
//   profile_record 1.21.9 - 1.21.11 GameProfile is a record: id() / name()
//   resourcelocation 1.21 - 1.21.10 net.minecraft.resources.ResourceLocation
//   identifier 1.21.11 renamed to net.minecraft.resources.Identifier
val patch = minecraftVersion.split(".").getOrNull(2)?.toInt() ?: 0

val compatDirs = listOf(
    if (patch >= 9) "profile_record" else "profile_legacy",
    if (patch >= 11) "identifier" else "resourcelocation"
)

sourceSets {
    main {
        java {
            compatDirs.forEach { srcDir("src/compat/$it/java") }
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersions.getValue(minecraftVersion)}")

    include(modImplementation("me.lucko:fabric-permissions-api:${permissionApiVersions.getValue(minecraftVersion)}")!!)
    include(modImplementation("eu.pb4:placeholder-api:${placeholderApiVersions.getValue(minecraftVersion)}")!!)
    include(implementation("org.postgresql:postgresql:42.7.3")!!)

    include(project(":common"))
    implementation(project(":common"))
}

tasks.processResources {
    inputs.properties(
        mapOf(
            "version" to project.version,
            "loader_version" to "0.19.3",
            "minecraft_version" to minecraftVersion
        )
    )

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "loader_version" to "0.19.3",
                "minecraft_version" to minecraftVersion
            )
        )
    }
}

tasks.register("buildAllVersions") {
    group = "build"
    description = "Builds the mod for all supported Minecraft 1.21.x versions."

    val root = rootDir
    val path = project.path

    doLast {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val gradlew = root.resolve(if (isWindows) "gradlew.bat" else "gradlew").absolutePath

        minecraftVersions.forEach { version ->
            println("======================================")
            println("Building Minecraft $version")
            println("======================================")

            val process = ProcessBuilder(
                gradlew,
                "$path:build",
                "-PminecraftVersion=$version"
            )
                .directory(root)
                .inheritIO()
                .start()

            val result = process.waitFor()

            if (result != 0) {
                throw GradleException("Build failed for Minecraft $version")
            }

            println("Finished Minecraft $version")
        }
    }
}
