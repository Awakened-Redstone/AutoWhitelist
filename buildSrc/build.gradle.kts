plugins {
	`kotlin-dsl`
	kotlin("jvm") version "2.1.21"
	kotlin("plugin.serialization") version "2.1.21"
}

repositories {
	mavenCentral()
	gradlePluginPortal()

	maven("https://maven.fabricmc.net/")
	maven("https://maven.kikugie.dev/releases")
	maven("https://maven.kikugie.dev/snapshots")
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation("dev.kikugie:stonecutter:0.8+")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
