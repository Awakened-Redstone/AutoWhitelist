import com.awakenedredstone.commons.bom
import org.jetbrains.kotlin.konan.file.use
import java.net.URI

plugins {
	idea
	eclipse
	`java-gradle-plugin`
	embeddedKotlin("jvm")
	embeddedKotlin("plugin.serialization")
	id("com.awakenedredstone.commons")
}

group = "com.awakenedredstone"
val name = "multiversion"
version = "0.1.0"

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
	maven("https://maven.kikugie.dev/releases")
	maven("https://maven.kikugie.dev/snapshots")
}

dependencies {
	compileOnly(gradleKotlinDsl())

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

	implementation("com.awakenedredstone:commons:0.1.2") { isTransitive = false }

	api("dev.kikugie:stonecutter:0.9.7")
	api("net.fabricmc:fabric-loom:1.17.20")

	bom("tools.jackson:jackson-bom:3.1.3")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("tools.jackson.dataformat:jackson-dataformat-xml")
	implementation("tools.jackson.dataformat:jackson-dataformat-yaml")
	implementation("tools.jackson.dataformat:jackson-dataformat-toml")
	implementation("tools.jackson.dataformat:jackson-dataformat-properties")
}

gradlePlugin {
	plugins.create(name) {
		id = "$group.$name"
		implementationClass = "$group.$name.MultiversionPlugin"
		version = project.version
	}
}

tasks.jar {
	manifest {
		attributes(mapOf("Implementation-Version" to version))
	}
}

tasks.named<Wrapper>("wrapper") {
	distributionType = Wrapper.DistributionType.ALL
}

/**
 * Run this task to download the Gradle sources next to the api jar, you may need to manually attach the sources jar
 */
tasks.register("downloadGradleSources") {
	doLast {
		// Awful hack to find the gradle api location
		val gradleApiFile = project.configurations.detachedConfiguration(dependencies.gradleApi()).files.find {
			it.name.startsWith("gradle-api")
		}!!

		val gradleApiSources = File(gradleApiFile.absolutePath.replace(".jar", "-sources.jar"))
		val url = "https://services.gradle.org/distributions/gradle-${GradleVersion.current().version}-src.zip"

		gradleApiSources.delete()

		println("Downloading (${url}) to (${gradleApiSources})")

		URI(url).toURL().openStream().use { data ->
			gradleApiSources.outputStream().use(data::copyTo)
		}
	}
}
