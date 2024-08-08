plugins {
    id("idea")
    id("net.neoforged.moddev") version "1.0.15"
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val PARCHMENT_VERSION: String? by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName = "sodium-neoforge"
}

sourceSets {
    val service = create("service")

    service.apply {
        compileClasspath += main.get().compileClasspath
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
    }

    main.get().apply {
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
        runtimeClasspath += project(":common").sourceSets.getByName("workarounds").output
    }
}

repositories {
    maven("https://maven.pkg.github.com/ims212/Forge_Fabric_API") {
        credentials {
            username = "IMS212"
            // Read only token
            password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
        }
    }
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val serviceJar: Jar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("service").output)
    from(project(":common").sourceSets.getByName("workarounds").output)
    from(rootDir.resolve("LICENSE.md"))
    manifest.attributes["FMLModType"] = "LIBRARY"
    archiveClassifier = "service"
}

configurations {
    create("serviceConfig") {
        isCanBeConsumed = true
        isCanBeResolved = false
        outgoing {
            artifact(serviceJar)
        }
    }
}

dependencies {
    jarJar(project(":neoforge", "serviceConfig"))
}

tasks.jar {
    val api = project.project(":common").sourceSets.getByName("api")
    from(api.output.classesDirs)
    from(api.output.resourcesDir)

    val main = project.project(":common").sourceSets.getByName("main")
    from(main.output.classesDirs) {
        exclude("/sodium.refmap.json")
    }
    from(main.output.resourcesDir)

    val desktop = project.project(":common").sourceSets.getByName("desktop")
    from(desktop.output.classesDirs)
    from(desktop.output.resourcesDir)

    from(rootDir.resolve("LICENSE.md"))

    filesMatching("neoforge.mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }

    manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
}

neoForge {
    // Specify the version of NeoForge to use.
    version = NEOFORGE_VERSION

    parchment {
        mappingsVersion = PARCHMENT_VERSION
        minecraftVersion = MINECRAFT_VERSION
    }

    runs {
        create("client") {
            client()
        }
    }

    mods {
        create("sodium") {
            sourceSet(sourceSets.main.get())
            sourceSet(project.project(":common").sourceSets.main.get())
            sourceSet(project.project(":common").sourceSets.getByName("api"))
        }

        create("sodiumservice") {
            sourceSet(sourceSets["service"])
            sourceSet(project(":common").sourceSets["workarounds"])
        }
    }
}

fun includeDep(dependency: String, closure: Action<ExternalModuleDependency>) {
    dependencies.implementation(dependency, closure)
    dependencies.jarJar(dependency, closure)
}

fun includeDep(dependency: String) {
    dependencies.implementation(dependency)
    dependencies.jarJar(dependency)
}

tasks.named("compileTestJava").configure {
    enabled = false
}

dependencies {
    compileOnly(project.project(":common").sourceSets.main.get().output)
    compileOnly(project.project(":common").sourceSets.getByName("api").output)
    includeDep("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1")
    includeDep("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.4.0+acb05a39d1")
    includeDep("net.fabricmc:fabric_rendering_data_attachment_v1:0.3.46+${MINECRAFT_VERSION}") {
        isTransitive = false
    }
    includeDep("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8cd1")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)