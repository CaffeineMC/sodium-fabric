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

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentNeoForge: Configuration by configurations.getting

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
base.archivesName.set("sodium-forge")

loom {
    silentMojangMappingsLicense()

    accessWidenerPath = project(":common").loom.accessWidenerPath
}

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentNeoForge.extendsFrom(common)
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
    atAccessWideners.add("sodium.accesswidener")
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

    include(implementation(group = "com.lodborg", name = "interval-tree", version = "1.0.0"))
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
        manifest.attributes["FMLModType"] = "SERVICE"
    }
}