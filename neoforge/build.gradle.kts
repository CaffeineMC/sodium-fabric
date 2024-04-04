import org.gradle.plugins.ide.idea.model.IdeaModule

plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.gradle.userdev") version "7.0.81"
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra

base {
    archivesName = "sodium-neoforge-${MINECRAFT_VERSION}"
}

sourceSets {
    val service = create("service")
    val main = getByName("main")

    service.apply {
        java {
            srcDir("src/service/java")
        }

        compileClasspath += main.compileClasspath
    }

    main.apply {
        //runtimeClasspath -= output
    }
}

// Automatically enable neoforge AccessTransformers if the file exists
// This location is hardcoded in FML and can not be changed.
// https://github.com/neoforged/FancyModLoader/blob/a952595eaaddd571fbc53f43847680b00894e0c1/loader/src/main/java/net/neoforged/fml/loading/moddiscovery/ModFile.java#L118
if (file("src/main/resources/META-INF/accesstransformer.cfg").exists()) {
    minecraft.accessTransformers.file("src/main/resources/META-INF/accesstransformer.cfg")
}

val fullJar: Jar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("service").output)
    from(project(":common").sourceSets.getByName("desktop").output)
    // Despite not being part of jarjar metadata, the mod jar must be located in this directory
    // in order to be deobfuscated by FG in userdev environments
    into("META-INF/jarjar/") {
        from(tasks.jar)
    }
    manifest {
        from(tasks.jar.get().manifest)
    }

    into("META-INF") {
       // from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("META-INF").resolve("mods.toml"))
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

sourceSets.main.get().resources { srcDir("src/generated/resources") }

dependencies {
    implementation("net.neoforged:neoforge:${NEOFORGE_VERSION}")
    compileOnly(project(":common"))
    implementation(files("fabric_renderer_api_v1-3.2.1.jar"))
    implementation(files("fabric_api_base-0.4.31.jar"))
    implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0")
}

// NeoGradle implementations the game, but we don"t want to add our common code to the game"s code
val notNeoTask: (Task) -> Boolean = { it : Task -> !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

tasks.jar {
    archiveClassifier = "mod"
}

tasks.build {
    dependsOn(fullJar)
}

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
    from(project(":common").sourceSets.getByName("api").resources)
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