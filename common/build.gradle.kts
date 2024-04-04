architectury {
    common("fabric", "neoforge")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra

dependencies {
    // We depend on Fabric Loader here for Mixin.
    modImplementation("net.fabricmc:fabric-loader:${FABRIC_LOADER_VERSION}")
    modCompileOnly("net.fabricmc.fabric-api:fabric-renderer-api-v1:3.2.9+1172e897d7")
    implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0")
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
        from("${rootProject.projectDir}/COPYING")
        from("${rootProject.projectDir}/COPYING.LESSER")

        val api = sourceSets.getByName("api")
        from(api.output.classesDirs)
        from(api.output.resourcesDir)

        manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    }
}