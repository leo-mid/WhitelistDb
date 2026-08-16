plugins {
    id("net.fabricmc.fabric-loom")
    java
}

val minecraftVersions = listOf(
    "26.2",
    "26.1.2",
    "26.1.1",
    "26.1"
)

val minecraftVersion = providers.gradleProperty("minecraftVersion").orElse("26.2").get()

base {
    archivesName.set("${project.property("archives_base_name")}-fabric-$minecraftVersion")
}

val fabricApiVersions = mapOf(
    "26.2" to "0.154.2+26.2",
    "26.1.2" to "0.154.2+26.1.2",
    "26.1.1" to "0.145.4+26.1.1",
    "26.1" to "0.145.1+26.1"
)

val permissionApiVersions = mapOf(
    "26.2" to "0.7.0",
    "26.1.2" to "0.7.0",
    "26.1.1" to "0.7.0",
    "26.1" to "0.7.0"
)

val placeholderApiVersions = mapOf(
    "26.2" to "3.0.0+26.1",
    "26.1.2" to "3.0.0+26.1",
    "26.1.1" to "3.0.0+26.1",
    "26.1" to "3.0.0+26.1"
)

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersions.getValue(minecraftVersion)}")

    include(implementation("me.lucko:fabric-permissions-api:${permissionApiVersions.getValue(minecraftVersion)}")!!)
    include(implementation("eu.pb4:placeholder-api:${placeholderApiVersions.getValue(minecraftVersion)}")!!)
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
    description = "Builds the mod for all supported Minecraft versions."

    doLast {
        minecraftVersions.forEach { version ->
            println("======================================")
            println("Building Minecraft $version")
            println("======================================")

            val process = ProcessBuilder(
                "./gradlew",
                "build",
                "-PminecraftVersion=$version"
            )
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