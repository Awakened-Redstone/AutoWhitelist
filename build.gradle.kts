import com.modrinth.minotaur.dependencies.ModDependency
import groovy.json.JsonSlurper
import java.io.File

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://maven.wispforest.io")
    maven("https://maven.nucleoid.xyz")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://m2.chew.pro/snapshots")
}

val CHANGELOG: String =
    if (file("CHANGELOG.md").exists()) {
        file("CHANGELOG.md").readText()
    } else {
        "No changelog provided"
    }
val minecraftVersion: String = stonecutter.current.version

var javaVer = "17"
if (isOrNewer("1.20.5")) {
    javaVer = "21"
}

@Suppress("UNCHECKED_CAST")
val modVersions: Map<String, List<String>> = JsonSlurper().parse(file("versions/modrinth.json")) as Map<String, List<String>>
val modVersion: String = property("mod_version").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}
var archivesBaseName: String = property("archives_base_name").toString()
version = "${property("mod_version")}+$minecraftVersion"
group = property("maven_group") as String

configurations.configureEach {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${property("loader_version")}")
    }
}

fun compareVer(ver1: String, ver2: String): Int {
    return VersionNumber.parse(ver1).compareTo(VersionNumber.parse(ver2))
}

fun isOrNewer(version: String): Boolean {
    return compareVer(stonecutter.current.version, version) >= 0;
}

fun isOrOlder(version: String): Boolean {
    return compareVer(stonecutter.current.version, version) <= 0;
}

fun file(path: String): File {
    return rootProject.file(path)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Mod dependencies
    include(modImplementation("eu.pb4:placeholder-api:${property("placeholder_api_version")}") {
        exclude(group = "net.fabricmc", module = "fabric-api")
    })
    if (isOrOlder("1.19")) {
        include(modImplementation("fr.catcore:server-translations-api:${property("translation_api_version")}") {
            exclude(group = "net.fabricmc", module = "fabric-api")
        })
    } else {
        include(modImplementation("xyz.nucleoid:server-translations-api:${property("translation_api_version")}") {
            exclude(group = "net.fabricmc", module = "fabric-api")
        })
    }

    // Libraries
    include(api("blue.endless:jankson:${property("jankson_version")}") as Any)
    api("pw.chew:jda-chewtils:${property("chewtils_version")}")
    include(api("net.dv8tion:JDA:${property("jda_version")}") {
        exclude(module = "opus-java")
    })

    // JDA dependencies
    include("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    include("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    include("com.fasterxml.jackson.core:jackson-core:2.17.0")
    include("com.neovisionaries:nv-websocket-client:2.14")
    include("org.apache.commons:commons-collections4:4.4")
    include("com.squareup.okhttp3:okhttp:4.12.0")
    include("com.squareup.okio:okio-jvm:2.13.0")
    include("net.sf.trove4j:core:3.1.0")

    // Chewtils
    include("pw.chew:jda-chewtils-command:${property("chewtils_version")}")
    include("pw.chew:jda-chewtils-commons:${property("chewtils_version")}")

    // Runtime only
    modRuntimeOnly("net.fabricmc:fabric-language-kotlin:${property("kotlin_version")}")

    // Compile only
    modCompileOnly("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
}

loom {
    runConfigs.getByName("server") {
        ideConfigGenerated(true)
        runDir("../../run")
    }
    runConfigs.getByName("client") {
        ideConfigGenerated(false)
        runDir("../../run")
    }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "build"
        dependsOn(tasks.named("build"))
    }
}

tasks.compileJava {
    sourceCompatibility = javaVer
    targetCompatibility = javaVer
    options.encoding = "UTF-8"

}

tasks.processResources {
    val versions = JsonSlurper().parse(file("versions/versions.json")) as Map<String, String>
    val map = mapOf(
        "version" to version,
        "minecraft" to versions[minecraftVersion],
        "fabric_api" to (if (isOrNewer("1.19.3")) "fabric-api" else "fabric")
    )

    inputs.properties(map)

    filesMatching("fabric.mod.json") {
        expand(map)
    }
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
tasks.register<Jar>("sourcesJar") {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.jar {
    from("LICENSE")
}

tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapMavenJar") {
    dependsOn(tasks.jar)
    inputFile.set(tasks.jar.get().archiveFile)
    archiveFileName.set("${archivesBaseName}-${version}-maven.jar")
    addNestedDependencies.set(false)
}

tasks.build.get().dependsOn(tasks.getByName("remapMavenJar"))

modrinth {
    val projectVersion: String = property("mod_version").toString()
    val projectVersionNumber: List<String> = projectVersion.split(Regex("-"), 2)

    var releaseName = "Release ${projectVersionNumber[0]}"
    if (projectVersion.contains("beta")) {
        val projectBeta: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        releaseName = "${projectVersionNumber[0]} - Beta ${projectBeta[1]}"
        versionType = "beta"
    } else if (projectVersion.contains("alpha")) {
        val projectAlpha: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        releaseName = "${projectVersionNumber[0]} - Alpha ${projectAlpha[1]}"
        versionType = "alpha"
    } else if (projectVersion.contains("rc")) {
        val projectRC: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        releaseName = "${projectVersionNumber[0]} - Release Candidate ${projectRC[1]}"
        versionType = "beta"
    }

    if (modVersions[minecraftVersion] == null) {
        throw Throwable("Please update modrinth.json, missing $minecraftVersion")
    }

    token = System.getenv("MODRINTH_TOKEN")
    projectId = "BMaqFQAd"
    versionName = "[$minecraftVersion] $releaseName"
    changelog = CHANGELOG
    uploadFile = tasks.getByName("remapJar")
    gameVersions = modVersions[minecraftVersion]
    syncBodyFrom = file("README.md").readText()
    dependencies = listOf(
        ModDependency("fabric-api", "required"),
        ModDependency("fabric-language-kotlin", "required"),
        ModDependency("luckperms", "optional"),
        ModDependency("placeholder-api", "embedded")
    )
}
