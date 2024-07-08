plugins {
    id("idea")
    id("net.neoforged.moddev") version "0.1.126"
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
    from(project(":common").sourceSets.getByName("desktop").output)
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
    compileOnly(project(":common"))
    includeDep("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1")
    includeDep("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.3.0+e3455cb4d1")
    includeDep("net.fabricmc:fabric_rendering_data_attachment_v1:0.3.46+${MINECRAFT_VERSION}") {
        isTransitive = false
    }
    additionalRuntimeClasspath("com.lodborg:interval-tree:1.0.0")
    includeDep("com.lodborg:interval-tree:1.0.0")
    includeDep("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8cd1")
}

// NeoGradle compiles the game, but we don't want to add our common code to the game's code
val notNeoTask: (Task) -> Boolean = { it: Task -> !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

tasks.withType<JavaCompile>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allSource)
    source(project(":common").sourceSets.getByName("api").allSource)
}

tasks.withType<Javadoc>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allJava)
    source(project(":common").sourceSets.getByName("api").allJava)
}

tasks.withType<ProcessResources>().matching(notNeoTask).configureEach {
    from(project(":common").sourceSets.main.get().resources)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)