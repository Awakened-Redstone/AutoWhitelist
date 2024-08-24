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
        versions("1.18", "1.18.2", "1.19", "1.19.4", "1.20", "1.20.2", "1.20.3", "1.20.5", "1.21")
    }
    kotlinController = true
    create(rootProject)
}
