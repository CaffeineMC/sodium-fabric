plugins {
    java
    idea
    `maven-publish`
    id("fabric-loom") version("1.6.5")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra


base {
    archivesName.set("sodium-fabric-${MINECRAFT_VERSION}")
}

dependencies {
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

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
    include(implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0"))

    implementation("com.google.code.findbugs:jsr305:3.0.1")
    compileOnly(project(":common"))
}

loom {
    if (project(":common").file("src/main/resources/sodium.accesswidener").exists())
        accessWidenerPath.set(project(":common").file("src/main/resources/sodium.accesswidener"))

    @Suppress("UnstableApiUsage")
    mixin { defaultRefmapName.set("sodium.refmap.json") }

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    withType<JavaCompile> {
        source(project(":common").sourceSets.main.get().allSource)
        source(project(":common").sourceSets.getByName("api").allSource)
        source(project(":common").sourceSets.getByName("workarounds").allSource)
    }

    javadoc { source(project(":common").sourceSets.main.get().allJava) }

    processResources {
        from(project(":common").sourceSets.main.get().resources) {
            exclude("sodium.accesswidener")
        }

        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    jar {
        from(rootDir.resolve("LICENSE.md"))
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}