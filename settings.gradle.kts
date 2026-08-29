pluginManagement {
    includeBuild("plugins/multiversion")

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }

    versionCatalogs {
        create("ft") {
            from("dev.kikugie.fletching-table:fletching-table.catalog:0.2-SNAPSHOT")
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("com.awakenedredstone.multiversion")
}

multiversion {
    loomVersion = "1.17-SNAPSHOT"
}

includeBuild("annotation-processors/prechecks")
