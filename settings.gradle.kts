pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8+"
}


stonecutter {
    create(rootProject) {
        versions("1.20", "1.21", "1.21.2", "1.21.5", "1.21.6", "1.21.9")
    }
}
