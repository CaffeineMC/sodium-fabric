import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    // Unlike most projects, we choose to pin the specific version of Loom.
    // This prevents a lot of issues where the build script can fail randomly because the Fabric Maven server
    // is not reachable for some reason, and it makes builds much more reproducible. Observation also shows that it
    // really helps to improve startup times on slow connections.
    id("architectury-plugin") version "3.4.151"
    id("dev.architectury.loom") version "1.5.388" apply false
}

val MINECRAFT_VERSION by extra { "1.20.4" }
val NEOFORGE_VERSION by extra { "20.4.219" }
val FABRIC_LOADER_VERSION by extra { "0.15.6" }
val FABRIC_API_VERSION by extra { "0.96.0+1.20.4" }

// https://semver.org/
val MOD_VERSION by extra { "0.6.0" }

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
}

subprojects {
    apply(plugin = "dev.architectury.loom")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    dependencies {
        "minecraft"(group = "com.mojang", name = "minecraft", version = MINECRAFT_VERSION)
        "mappings"(loom.officialMojangMappings())
    }
}

architectury {
    minecraft = MINECRAFT_VERSION
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

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

    version = createVersionString()
    group = "net.caffeinemc.mods"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
}
