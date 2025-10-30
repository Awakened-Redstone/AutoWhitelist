plugins {
	`kotlin-dsl`
}

group = "com.awakenedredstone"
val name = "commons"
version = "0.1.0"

repositories {
	mavenCentral()
	gradlePluginPortal()
}

dependencies {
	implementation(kotlin("stdlib"))
}

gradlePlugin {
	plugins {
		create(name) {
			id = "$group.$name"
			implementationClass = "$group.$name.PluginInit"
		}
	}
}
