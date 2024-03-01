plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

repositories {
    maven {
        url = uri("https://maven.neoforged.net/releases")
    }

    mavenLocal()
}
val developmentNeoForge: Configuration by configurations.getting
val architecturyTransformerRuntimeClasspath: Configuration by configurations.getting

sourceSets {
    val service = create("service")
    val main = getByName("main")

    service.apply {
        java {
            srcDir("src/service/java")
        }

        compileClasspath  += main.compileClasspath
    }

    main.apply {
        runtimeClasspath -= output
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
base.archivesName.set("sodium-forge")

loom {
    silentMojangMappingsLicense()

    accessWidenerPath = project(":common").loom.accessWidenerPath
}

configurations {
    compileOnly.configure { extendsFrom(common) }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

var fullJar = tasks.register<Jar>("fullJar")

fullJar.configure {
    dependsOn(tasks.remapJar)
    from(sourceSets.getByName("service").output)
    manifest.from(tasks.remapJar.get().manifest)
    into("META-INF") {
        from(sourceSets.getByName("main").output.resourcesDir!!.toPath().resolve("META-INF").resolve("mods.toml").toFile())
    }
    into("META-INF/jarjar") {
        from(tasks.remapJar.get().archiveFile.get())
    }

    archiveClassifier = ""

    manifest.attributes["FMLModType"] = "LIBRARY"

}

var runClientJar = tasks.register<Jar>("runClientJar")

runClientJar.configure {
    dependsOn(tasks.shadowJar)
    from(sourceSets.getByName("service").output)
    manifest.from(tasks.shadowJar.get().manifest)
    into("META-INF") {
        from(sourceSets.getByName("main").output.resourcesDir!!.toPath().resolve("META-INF").resolve("mods.toml").toFile())
    }
    into("META-INF/jarjar") {
        from(tasks.shadowJar.get().archiveFile.get())
    }
    destinationDirectory.set(projectDir.resolve("build").resolve("devlibs"))
    archiveClassifier = "devJar"

    manifest.attributes["FMLModType"] = "LIBRARY"
}

tasks.assemble.configure {
    dependsOn(fullJar)
    dependsOn(runClientJar)
}
tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
    atAccessWideners.add("sodium.accesswidener")
    archiveClassifier.set("modonly")
    destinationDirectory.set(projectDir.resolve("build").resolve("devlibs"))
}

tasks.runClient {
    classpath += files(runClientJar)
}

tasks.jar {
    archiveClassifier.set("dev")
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${NEOFORGE_VERSION}")

    include(group = "com.lodborg", name = "interval-tree", version = "1.0.0")
    forgeRuntimeLibrary(group = "com.lodborg", name = "interval-tree", version = "1.0.0")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionNeoForge")) { isTransitive = false }
}

tasks.processResources {
    filesMatching("META-INF/mods.toml") {
        expand(mapOf("version" to project.version))
    }
}

tasks {
    jar {
        from("${rootProject.projectDir}/COPYING")
        from("${rootProject.projectDir}/COPYING.LESSER")

        manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
        manifest.attributes["Automatic-Module-Name"] = "net.caffeinemc.mods.sodium.cursed"
    }
}