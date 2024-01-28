rootProject.name = "sodium"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }

        mavenCentral()
        gradlePluginPortal()
    }
}