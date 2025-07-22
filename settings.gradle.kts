import dev.kikugie.stonecutter.StonecutterSettings

pluginManagement {
    repositories {
        // FabricMC
        maven("https://maven.fabricmc.net/")
        // Stonecutter Releases
        maven("https://maven.kikugie.dev/releases")
        // Stonecutter Snaphots
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.4.3"
}


extensions.configure<StonecutterSettings> {
    centralScript = "build.gradle.kts"
    shared {
        versions("1.20", "1.21", "1.21.2", "1.21.5", "1.21.6")
    }
    kotlinController = true
    create(rootProject)
}
