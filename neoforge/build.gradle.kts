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
    maven { url = uri("https://maven.neoforged.net/releases/") }

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

dependencies {
    compileOnly(project(":common"))
    implementation("net.fabricmc:fabric_api_base:0.4.40+${MINECRAFT_VERSION}")
    jarJar("net.fabricmc:fabric_api_base:0.4.40+${MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric_renderer_api_v1:3.2.12+${MINECRAFT_VERSION}")
    jarJar("net.fabricmc:fabric_renderer_api_v1:3.2.12+${MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric_rendering_data_attachment_v1:0.3.46+${MINECRAFT_VERSION}")
    jarJar("net.fabricmc:fabric_rendering_data_attachment_v1:0.3.46+${MINECRAFT_VERSION}")
    implementation("com.lodborg:interval-tree:1.0.0")
    jarJar("com.lodborg:interval-tree:[1.0.0,1.0.1)")
    implementation("net.fabricmc:fabric_block_view_api_v2:1.0.8+${MINECRAFT_VERSION}")
    jarJar("net.fabricmc:fabric_block_view_api_v2:1.0.8+${MINECRAFT_VERSION}")
    compileOnly("maven.modrinth:immersiveengineering:11mMmtHT")
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