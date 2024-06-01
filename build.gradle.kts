
plugins {
    id("java")
    id("fabric-loom") version("1.6.6") apply(false)
}

val MINECRAFT_VERSION by extra { "1.20.1" }
val NEOFORGE_VERSION by extra { "47.2.32" }
val FABRIC_LOADER_VERSION by extra { "0.15.11" }
val FABRIC_API_VERSION by extra { "0.92.2+1.20.1" }

// https://semver.org/
val MOD_VERSION by extra { "0.6.0" }

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

subprojects {
    apply(plugin = "maven-publish")

    java.toolchain.languageVersion = JavaLanguageVersion.of(17)


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

    tasks.processResources {
        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to createVersionString()))
        }
    }

    version = createVersionString()
    group = "net.caffeinemc.mods"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    // Disables Gradle's custom module metadata from being published to maven. The
    // metadata includes mapped dependencies which are not reasonably consumable by
    // other mod developers.
    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}