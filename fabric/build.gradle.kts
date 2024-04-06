plugins {
    id("java")
    id("idea")
    id("fabric-loom") version "1.6.5"
    id("maven-publish")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra

base.archivesName.set("sodium-fabric")

val commonMain = project(":common").sourceSets.main.get()
val commonApi = project(":common").sourceSets.getByName("api")
val commonWorkarounds = project(":common").sourceSets.getByName("workarounds")

sourceSets {
    main {
        compileClasspath += commonWorkarounds.output
        runtimeClasspath += commonWorkarounds.output
    }
}

loom {
     accessWidenerPath = project(":common").loom.accessWidenerPath
}

dependencies {
    "minecraft"(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
    "mappings"(loom.officialMojangMappings())
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = FABRIC_LOADER_VERSION)
    include(implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0"))

    fun addEmbeddedFabricModule(name: String) {
        val module = fabricApi.module(name, FABRIC_API_VERSION)
        modImplementation(module)
        include(module)
    }

    // Fabric API modules
    addEmbeddedFabricModule("fabric-api-base")
    addEmbeddedFabricModule("fabric-block-view-api-v2")
    addEmbeddedFabricModule("fabric-renderer-api-v1")
    addEmbeddedFabricModule("fabric-rendering-data-attachment-v1")
    addEmbeddedFabricModule("fabric-rendering-fluids-v1")
    addEmbeddedFabricModule("fabric-resource-loader-v0")

    implementation(project(":common", "namedElements")) { isTransitive = false }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.WARN

    inputs.property("version", project.version)
    from(project(":common").tasks.getByName("processResources"))

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.remapJar {
    archiveClassifier.set(null as String?)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.WARN

    archiveClassifier.set("dev")
    from(commonMain.output)
    from(commonApi.output)
    from(commonWorkarounds.output)
}