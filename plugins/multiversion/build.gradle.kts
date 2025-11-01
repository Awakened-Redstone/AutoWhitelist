plugins {
	`kotlin-dsl`
	kotlin("jvm") version "2.1.21"
	kotlin("plugin.serialization") version "2.1.21"
}

group = "com.awakenedredstone"
val name = "multiversion"
version = "0.1.0"

repositories {
	mavenCentral()
	gradlePluginPortal()

	maven("https://maven.fabricmc.net/")
	maven("https://maven.kikugie.dev/releases")
	maven("https://maven.kikugie.dev/snapshots")
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(kotlin("reflect"))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

	implementation("com.awakenedredstone:commons:0.1.0")

	implementation("dev.kikugie:stonecutter:0.8+")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

}

gradlePlugin {
	plugins {
		create(name) {
			id = "$group.$name"
			implementationClass = "$group.$name.PluginInit"
		}
	}
}
