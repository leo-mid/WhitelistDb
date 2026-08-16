plugins {
    id("net.fabricmc.fabric-loom")
    java
}

val minecraftVersion = "1.21.11"

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
    "1.21.2" to "2.4.2+1.21",
    "1.21.1" to "2.4.2+1.21",
    "1.21" to "2.4.2+1.21"
)

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
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