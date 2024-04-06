import org.gradle.plugins.ide.idea.model.IdeaModule

plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.gradle.userdev") version "7.0.81"
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

base {
    archivesName = "sodium-neoforge-${MINECRAFT_VERSION}"
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://maven.pkg.github.com/ims212/forge-frapi")
                credentials {
                    username = "IMS212"
                    // Read only token
                    password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
                }
            }
        }
        filter {
            // this repository *only* contains artifacts with group "my.company"
            includeGroup("net.caffeinemc")
        }
    }
}

sourceSets {
    val service = create("service")
    val main = getByName("main")

    service.apply {
        java {
            srcDir("src/service/java")
        }

        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
        compileClasspath += main.compileClasspath
    }

    main.apply {
        //runtimeClasspath -= output
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
        compileClasspath += project(":common").sourceSets.getByName("api").output
        compileClasspath += project(":common").sourceSets.getByName("main").output
    }
}

val jijImplementation = configurations.create("jijImplementation")
jarJar.enable()
configurations {
    jijImplementation
}
dependencies {
    implementation("net.neoforged:neoforge:${NEOFORGE_VERSION}")
    compileOnly(project(":common"))
    implementation(group = "net.caffeinemc", name = "fabric_api_base", version = "0.4.32")
    jarJar(group = "net.caffeinemc", name = "fabric_api_base", version = "[0.4.32, 0.4.33)")
    implementation(group = "net.caffeinemc", name = "fabric_renderer_api_v1", version = "3.2.1")
    jarJar(group = "net.caffeinemc", name = "fabric_renderer_api_v1", version = "[3.2.1, 3.2.2)")
    implementation(group = "net.caffeinemc", name = "fabric_rendering_data_attachment_v1", version = "0.3.37")
    jarJar(group = "net.caffeinemc", name = "fabric_rendering_data_attachment_v1", version = "[0.3.37, 0.3.38)")
    implementation(group = "net.caffeinemc", name = "fabric_block_view_api_v2", version = "1.0.1")
    jarJar(group = "net.caffeinemc", name = "fabric_block_view_api_v2", version = "[1.0.1, 1.0.2)")
    implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0")
    jarJar(group = "com.lodborg", name = "interval-tree", version = "[1.0,2.0)")
}
// Automatically enable neoforge AccessTransformers if the file exists
// This location is hardcoded in FML and can not be changed.
// https://github.com/neoforged/FancyModLoader/blob/a952595eaaddd571fbc53f43847680b00894e0c1/loader/src/main/java/net/neoforged/fml/loading/moddiscovery/ModFile.java#L118
if (file("src/main/resources/META-INF/accesstransformer.cfg").exists()) {
    minecraft.accessTransformers.file("src/main/resources/META-INF/accesstransformer.cfg")
}

fun createVersionString(): String {
    val builder = StringBuilder()

    val isReleaseBuild = project.hasProperty("build.release")
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    if (isReleaseBuild) {
        builder.append(MOD_VERSION)
    } else {
        builder.append(MOD_VERSION.substringBefore('-'))
        builder.append("-snapshot")
    }

    builder.append("+mc").append(MINECRAFT_VERSION)

    if (!isReleaseBuild) {
        if (buildId != null) {
            builder.append("-build.${buildId}")
        } else {
            builder.append("-local")
        }
    }

    return builder.toString()
}

tasks.jar {
    filesMatching("mods.toml") {
        println("Found a file!")
        expand(mapOf("version" to createVersionString()))
    }
    duplicatesStrategy = DuplicatesStrategy.WARN

    archiveClassifier = "mod"
    from(project(":common").sourceSets.getByName("main").output)
    from(project(":common").sourceSets.getByName("api").output)
}

tasks.jarJar {
    filesMatching("META-INF/mods.toml") {
        expand(mapOf("version" to createVersionString()))
    }
    from(project(":common").sourceSets.getByName("main").output)
    from(project(":common").sourceSets.getByName("api").output)
}

val fullJar: Jar by tasks.creating(Jar::class) {
    dependsOn(tasks.jarJar)
    from(sourceSets.getByName("service").output)
    from(project(":common").sourceSets.getByName("desktop").output)
    from(project(":common").sourceSets.getByName("workarounds").output)
    // Despite not being part of jarjar metadata, the mod jar must be located in this directory
    // in order to be deobfuscated by FG in userdev environments
    into("META-INF/jarjar/") {
        from(tasks.jarJar.get().archiveFile)
    }

    into("META-INF") {
        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("sodium-icon.png"))

        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("META-INF").resolve("mods.toml"))
    }
    filesMatching("mods.toml") {
        expand(mapOf("version" to createVersionString()))
    }
    manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    manifest.attributes["FMLModType"] = "LIBRARY"

}

runs {
    configureEach {
        modSource(project.sourceSets.main.get())
    }

    create("client") {
        workingDirectory(project.file("run"))
        dependencies {
            runtime("com.lodborg:interval-tree:1.0.0")
            runtime(project(":common").sourceSets.getByName("main").output)
            runtime(project(":common").sourceSets.getByName("api").output)
            runtime(project(":common").sourceSets.getByName("workarounds").output)
        }
        //displayName = "Client"
        //setProperty("mixin.env.remapRefMap", "true")
        //setProperty("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
        //mods {
        //    create("modRun") {
        //        source(sourceSets.main.get())
        //        source(project(":common").sourceSets.main.get())
        //    }
        //}
    }
}



// NeoGradle implementations the game, but we don"t want to add our common code to the game"s code
val notNeoTask: (Task) -> Boolean = { it : Task -> !it.name.startsWith("neo") && !it.name.startsWith("compileService") }



tasks.build {

    dependsOn(fullJar)
}

tasks.processTestResources {
    filesMatching("mods.toml") {
        expand(mapOf("version" to project.version))
    }
}

tasks.processResources {
    filesMatching("mods.toml") {
        expand(mapOf("version" to project.version))
    }
}


java.toolchain.languageVersion = JavaLanguageVersion.of(21)
publishing {
    publications {
       // mavenJava(MavenPublication) {
       //     artifactId base.archivesName.get()
       //     from components.java
       // }
    }
    repositories {
        maven(
                "file://"+System.getenv("local_maven"))
    }
}