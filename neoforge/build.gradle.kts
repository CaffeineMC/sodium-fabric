import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier

plugins {
    id("idea")
    id("maven-publish")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("java-library")
    id("org.spongepowered.mixin") version "0.7-SNAPSHOT"
}

base {
    archivesName = "sodium-forge-1.20.1"
}

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

jarJar.enable()

mixin {
    add(sourceSets.main.get(), "sodium.refmap.json")
    add(project(":common").sourceSets.main.get(), "sodium.refmap.json")
    config("sodium.mixins.json")
    config("sodium-forge.mixins.json")
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

    test.get().apply {
        compileClasspath += main.get().compileClasspath
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
    }
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        forRepositories(fg.repository) // Only add this if you're using ForgeGradle, otherwise remove this line
        filter {
            includeGroup("maven.modrinth")
        }
    }

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
            includeGroup("net.caffeinemc.lts")
        }
    }
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://files.minecraftforge.net/maven/") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
    maven { url = uri("https://maven.su5ed.dev/releases") }
    mavenLocal()
    maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge Snapshots" }

}

minecraft {
    mappings("official", "1.20.1")
    copyIdeResources = true //Calls processResources when in dev

    val transformerFile = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (transformerFile.exists()) {
        accessTransformer(transformerFile)
    }

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            ideaModule("${rootProject.name}.${project.name}.main")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods {
                create("modRun") {
                    source(sourceSets.main.get())
                    source(project(":common").sourceSets.main.get())
                }
            }
        }

        create("data") {
            //programArguments.addAll("--mod", "sodium", "--all", "--output", file("src/generated/resources/").getAbsolutePath(), "--existing", file("src/main/resources/").getAbsolutePath())
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${MINECRAFT_VERSION}-${NEOFORGE_VERSION}")
    compileOnly(project(":common", "namedElements"))
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")

    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.3.5")) {
        jarJar.ranged(this, "[0.3.5,)")
    }
    implementation(fg.deobf("dev.su5ed.sinytra.fabric-api:fabric-api-base:0.4.31+ef105b4977"))
    jarJar("dev.su5ed.sinytra.fabric-api:fabric-api-base:[0.4.30,0.4.32)") {
        isTransitive = false
        jarJar.pin(this, "0.4.31+ef105b4977")
    }
    implementation(fg.deobf("dev.su5ed.sinytra.fabric-api:fabric-renderer-api-v1:3.2.1+1d29b44577"))
    jarJar("dev.su5ed.sinytra.fabric-api:fabric-renderer-api-v1:[3.2.1,3.2.2)"){
        isTransitive = false
    }
    implementation(fg.deobf("dev.su5ed.sinytra.fabric-api:fabric-rendering-data-attachment-v1:0.3.37+a6081afc77"))
    jarJar("dev.su5ed.sinytra.fabric-api:fabric-rendering-data-attachment-v1:[0.3.36,0.3.38)"){
        isTransitive = false
        jarJar.pin(this, "0.3.37+a6081afc77")
    }
    minecraftLibrary("com.lodborg:interval-tree:1.0.0")
    jarJar("com.lodborg:interval-tree:[1.0.0,1.0.1)")
    implementation(fg.deobf("dev.su5ed.sinytra.fabric-api:fabric-block-view-api-v2:1.0.1+0767707077"))
    jarJar("dev.su5ed.sinytra.fabric-api:fabric-block-view-api-v2:[1.0.1,1.0.2)") {
        isTransitive = false
        jarJar.pin(this, "1.0.1+0767707077")
    }
}


tasks.jarJar {
    dependsOn("reobfJar")
    archiveClassifier = ""
}

tasks.jar {
    archiveClassifier = "std"
}

val notNeoTask: (Task) -> Boolean = { it: Task ->
    !it.name.startsWith("compileService")
}

tasks {
    withType<JavaCompile>().matching(notNeoTask).configureEach {
        source(project(":common").sourceSets.main.get().allSource)
        source(project(":common").sourceSets.getByName("api").allSource)
        source(project(":common").sourceSets.getByName("workarounds").allSource)
    }

    javadoc { source(project(":common").sourceSets.main.get().allJava) }

    processResources { from(project(":common").sourceSets.main.get().resources) }

    jar { finalizedBy("reobfJar") }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
            fg.component(this)
        }
    }

    repositories {
        maven("file://${System.getenv("local_maven")}")
    }
}


sourceSets.forEach {
    val dir = layout.buildDirectory.dir("sourceSets/${it.name}")
    it.output.setResourcesDir(dir)
    it.java.destinationDirectory.set(dir)
}