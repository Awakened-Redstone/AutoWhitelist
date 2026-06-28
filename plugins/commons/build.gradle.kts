plugins {
	idea
	eclipse
	`java-gradle-plugin`
	embeddedKotlin("jvm")
}

group = "com.awakenedredstone"
val name = "commons"
version = "0.1.2"

idea {
	module {
		excludeDirs = setOf(*excludeDirs.toTypedArray(), file(".kotlin"))
	}
}

kotlin {
	compilerOptions {
		// allWarningsAsErrors = true
		// Loom has some nullable parameters and values that aren't annotated as nullable,
		// setting this to a warning lets us suppress incorrect inferences.
		// Since allWarningsAsErrors is set, failure to handle this warnings will still result in an error
		// It does make the build failure less clear but is the option for now
		freeCompilerArgs.add("-Xnullability-annotations=@org.jspecify.annotations:warn")
	}
}

repositories {
	mavenLocal()
	mavenCentral()

	maven("https://maven.fabricmc.net/")
}

dependencies {
	compileOnly(gradleKotlinDsl())

	implementation("net.fabricmc:fabric-loom:1.16.1")
}

gradlePlugin {
	plugins.create(name) {
		id = "$group.$name"
		implementationClass = "$group.$name.CommonsPlugin"
		version = project.version
	}
}

tasks.jar {
	manifest {
		attributes(mapOf("Implementation-Version" to version))
	}
}
