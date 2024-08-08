plugins {
    id("java")
    id("idea")
    id("fabric-loom") version ("1.7.3")
}

repositories {
    maven("https://maven.parchmentmc.org/")
}

val MINECRAFT_VERSION: String by rootProject.extra
val PARCHMENT_VERSION: String? by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
    mappings(loom.layered() {
        officialMojangMappings()
        if (PARCHMENT_VERSION != null) {
            parchment("org.parchmentmc.data:parchment-${MINECRAFT_VERSION}:${PARCHMENT_VERSION}@zip")
        }
    })
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    compileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")

    fun addDependentFabricModule(name: String) {
        val module = fabricApi.module(name, FABRIC_API_VERSION)
        modCompileOnly(module)
    }

    addDependentFabricModule("fabric-api-base")
    addDependentFabricModule("fabric-block-view-api-v2")
    addDependentFabricModule("fabric-renderer-api-v1")
    addDependentFabricModule("fabric-rendering-data-attachment-v1")

    modCompileOnly("net.fabricmc.fabric-api:fabric-renderer-api-v1:3.2.9+1172e897d7")
}

sourceSets {
    val main = getByName("main")
    val api = create("api")
    val workarounds = create("workarounds")
    val desktop = create("desktop")

    api.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    workarounds.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    desktop.apply {
        java {
            srcDir("src/desktop/java")
        }
    }

    main.apply {
        java {
            compileClasspath += api.output
            compileClasspath += workarounds.output
            runtimeClasspath += api.output
        }
    }
}

loom {
    mixin {
        defaultRefmapName = "sodium.refmap.json"
    }

    accessWidenerPath = file("src/main/resources/sodium.accesswidener")

    mods {
        val main by creating { // to match the default mod generated for Forge
            sourceSet("api")
            sourceSet("desktop")
            sourceSet("main")
        }
    }
}

tasks {
    getByName<JavaCompile>("compileDesktopJava") {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    jar {
        from(rootDir.resolve("LICENSE.md"))

        val api = sourceSets.getByName("api")
        from(api.output.classesDirs)
        from(api.output.resourcesDir)

        val workarounds = sourceSets.getByName("workarounds")
        from(workarounds.output.classesDirs)
        from(workarounds.output.resourcesDir)

        val desktop = sourceSets.getByName("desktop")
        from(desktop.output.classesDirs)
        from(desktop.output.resourcesDir)

        manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    }
}

// This trick hides common tasks in the IDEA list.
tasks.configureEach {
    group = null
}