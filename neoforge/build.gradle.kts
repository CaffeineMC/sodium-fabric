import groovy.lang.Closure

plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.moddev") version "0.1.110"
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
    }
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ims212/Forge_Fabric_API")
        credentials {
            username = "IMS212"
            // Read only token
            password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
        }
    }
    maven { url = uri("https://maven.su5ed.dev/releases") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
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

val fullJar: Jar by tasks.creating(Jar::class) {
    dependsOn(tasks.jar)
    from(sourceSets.getByName("service").output)
    from(project(":common").sourceSets.getByName("desktop").output)
    from(project(":common").sourceSets.getByName("workarounds").output)

    into("META-INF/jarjar/") {
        from(tasks.jar.get().archiveFile)
    }

    into("META-INF") {
        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("sodium-icon.png"))

        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("META-INF").resolve("neoforge.mods.toml"))
    }

    from(rootDir.resolve("LICENSE.md"))

    filesMatching("neoforge.mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }

    manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    manifest.attributes["FMLModType"] = "LIBRARY"

}

tasks.build {
    dependsOn.clear()
    dependsOn(fullJar)
}

tasks.jar {
    from(rootDir.resolve("LICENSE.md"))

    archiveClassifier = "modonly"
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
            additionalRuntimeClasspath.add("com.lodborg:interval-tree:1.0.0")
            additionalRuntimeClasspath.add(rootProject.project(":common").sourceSets.getByName("workarounds").output)
            client()
        }
    }

    mods {
        create("sodium") {
            sourceSet(sourceSets.main.get())
        }
    }
}

val localRuntime = configurations.create("localRuntime")

fun includeDep(dependency : String, closure : Action<ExternalModuleDependency>) {
    dependencies.implementation(dependency, closure)
    dependencies.jarJar(dependency, closure)
}

fun includeDep(dependency : String) {
    dependencies.implementation(dependency)
    dependencies.jarJar(dependency)
}

dependencies {
    compileOnly(project(":common"))
    includeDep("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1")
    includeDep("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.3.0+e3455cb4d1")
    includeDep("net.fabricmc:fabric_rendering_data_attachment_v1:0.3.46+${MINECRAFT_VERSION}") {
        isTransitive = false
    }
    includeDep("com.lodborg:interval-tree:1.0.0")
    includeDep("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8cd1")
}

// Sets up a dependency configuration called 'localRuntime'.
// This configuration should be used instead of 'runtimeOnly' to declare
// a dependency that will be present for runtime testing but that is
// "optional", meaning it will not be pulled by dependents of this mod.
configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
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

publishing {
    publications {

    }
    repositories {
        maven {
            url = uri("file://" + System.getenv("local_maven"))
        }
    }
}