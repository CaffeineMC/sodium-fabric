object Constants {
    // https://fabricmc.net/develop/
    const val MINECRAFT_VERSION: String = "1.21"
    const val FABRIC_LOADER_VERSION: String = "0.15.11"
    const val FABRIC_API_VERSION: String = "0.100.3+1.21"

    // https://semver.org/
    const val MOD_VERSION: String = "0.6.0"
}

plugins {
    // Unlike most projects, we choose to pin the specific version of Loom.
    // This prevents a lot of issues where the build script can fail randomly because the Fabric Maven server
    // is not reachable for some reason, and it makes builds much more reproducible. Observation also shows that it
    // really helps to improve startup times on slow connections.
    id("fabric-loom") version "1.6.5"
}

base {
    archivesName = "sodium-fabric"

    group = "net.caffeinemc.mods"
    version = createVersionString()
}

loom {
    mixin {
        defaultRefmapName = "sodium.refmap.json"
    }

    accessWidenerPath = file("src/main/resources/sodium.accesswidener")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    val main = getByName("main")
    val api = create("api")
    val desktop = create("desktop")

    api.apply {
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
            runtimeClasspath += api.output
        }
    }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = Constants.MINECRAFT_VERSION)
    mappings(loom.officialMojangMappings())
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = Constants.FABRIC_LOADER_VERSION)
    include(implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0"))

    fun addEmbeddedFabricModule(name: String) {
        val module = fabricApi.module(name, Constants.FABRIC_API_VERSION)
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
}

tasks {
    getByName<JavaCompile>("compileDesktopJava") {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    jar {
        from("${rootProject.projectDir}/LICENSE.md")

        val api = sourceSets.getByName("api")
        from(api.output.classesDirs)
        from(api.output.resourcesDir)

        val desktop = sourceSets.getByName("desktop")
        from(desktop.output.classesDirs)
        from(desktop.output.resourcesDir)

        manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

fun createVersionString(): String {
    val builder = StringBuilder()

    val isReleaseBuild = project.hasProperty("build.release")
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    if (isReleaseBuild) {
        builder.append(Constants.MOD_VERSION)
    } else {
        builder.append(Constants.MOD_VERSION.substringBefore('-'))
        builder.append("-snapshot")
    }

    builder.append("+mc").append(Constants.MINECRAFT_VERSION)

    if (!isReleaseBuild) {
        if (buildId != null) {
            builder.append("-build.${buildId}")
        } else {
            builder.append("-local")
        }
    }

    return builder.toString()
}
