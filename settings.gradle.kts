pluginManagement {
    includeBuild("plugins/multiversion")

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("com.awakenedredstone.multiversion")
}

includeBuild("annotation-processors/prechecks")
