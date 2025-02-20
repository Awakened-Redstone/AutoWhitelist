import com.modrinth.minotaur.dependencies.ModDependency
import groovy.json.JsonSlurper
import me.modmuss50.mpp.ReleaseType
import java.io.File

plugins {
    id("fabric-loom") version "1.9+"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://maven.wispforest.io")
    maven("https://maven.nucleoid.xyz")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://m2.chew.pro/releases")
}

val changelogText: String = if (file("CHANGELOG.md").exists()) {
        file("CHANGELOG.md").readText()
    } else {
        "No changelog provided"
    }
val minecraftVersion: String = stonecutter.current.version
val latestVersion: String = stonecutter.versions.last().version

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
    return compareVer(stonecutter.current.version, version) >= 0
}

fun isOrOlder(version: String): Boolean {
    return compareVer(stonecutter.current.version, version) <= 0
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
    include(modImplementation("xyz.nucleoid:server-translations-api:${property("translation_api_version")}") {
        exclude(group = "net.fabricmc", module = "fabric-api")
    })
    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:${property("mixinextras")}")!!)!!)

    // Libraries
    include(api("blue.endless:jankson:${property("jankson_version")}")!!)
    api("pw.chew:jda-chewtils:${property("chewtils_version")}") {
        exclude(module = "log4j-core")
    }
    include(api("net.dv8tion:JDA:${property("jda_version")}") {
        exclude(module = "opus-java")
        exclude(module = "log4j-core")
        exclude(module = "log4j-api")
        exclude(module = "slf4j-api")
    })

    // JDA dependencies
    include("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    include("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    include("com.fasterxml.jackson.core:jackson-core:2.17.2")
    include("com.neovisionaries:nv-websocket-client:2.14")
    include("com.google.crypto.tink:tink:1.14.1")
    include("com.squareup.okhttp3:okhttp:4.12.0")
    include("com.squareup.okio:okio-jvm:3.6.0")
    include("com.squareup.okio:okio:3.6.0")
    include("net.sf.trove4j:core:3.1.0")
    include("org.apache.commons:commons-collections4:4.4")
    include("org.json:json:20241224")

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
    accessWidenerPath.set(rootProject.file("src/main/resources/$archivesBaseName.accesswidener"))

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
    sourceCompatibility = "17"
    targetCompatibility = javaVer
    options.encoding = "UTF-8"
}

tasks.processResources {
    val versions = JsonSlurper().parse(file("versions/versions.json")) as Map<*, *>
    val map = mapOf(
        "version" to version,
        "minecraft" to versions[minecraftVersion]
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
    java.withSourcesJar()
}

tasks.build.get().dependsOn(tasks.getByName("remapMavenJar"))

val projectVersion: String = property("mod_version").toString()
val projectVersionNumber: List<String> = projectVersion.split(Regex("-"), 2)
var projectVersionName = "Release ${projectVersionNumber[0]}"
var projectVersionType = ReleaseType.STABLE
if (projectVersion.contains("beta")) {
    val projectBeta: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
    projectVersionName = "${projectVersionNumber[0]} - Beta ${projectBeta[1]}"
    projectVersionType = ReleaseType.BETA
} else if (projectVersion.contains("alpha")) {
    val projectAlpha: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
    projectVersionName = "${projectVersionNumber[0]} - Alpha ${projectAlpha[1]}"
    projectVersionType = ReleaseType.ALPHA
} else if (projectVersion.contains("rc")) {
    val projectRC: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
    projectVersionName = "${projectVersionNumber[0]} - Release Candidate ${projectRC[1]}"
    projectVersionType = ReleaseType.BETA
}

fun <T> action(action: Action<T>) : Action<T> where T : Task {
    return action
}

val checks: Action<Task> = action {
    if (modVersions[minecraftVersion] == null) {
        throw MissingResourceException("Please update modrinth.json, missing $minecraftVersion")
    }

    if (changelogText.isEmpty()) {
        throw MissingResourceException("Update the changelog!")
    }
}

tasks.getByName("modrinth").doFirst(checks)
tasks.getByName("modrinthSyncBody").doFirst(checks)
tasks.getByName("publishMods").doFirst(checks)

modrinth {
    versionType = projectVersionType.toString().lowercase()
    token = providers.gradleProperty("MODRINTH_TOKEN")
    projectId = "BMaqFQAd"
    versionName = "[$minecraftVersion] $projectVersionName"
    changelog = changelogText
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

publishMods {
    file = (tasks.getByName("remapJar") as AbstractArchiveTask).archiveFile
    changelog = changelogText
    type = projectVersionType
    modLoaders.add("fabric")
    displayName = "[$minecraftVersion] $projectVersionName"

    curseforge {
        projectId = "575422"
        projectSlug = "autowhitelist" // Required for discord webhook
        accessToken = providers.gradleProperty("CURSEFORGE_TOKEN")
        minecraftVersions = modVersions[minecraftVersion]
        changelog = changelogText
        requires("fabric-api", "fabric-language-kotlin")
        embeds("text-placeholder-api")
        optional("luckperms")
    }

    modrinth {
        projectId = "BMaqFQAd"
        accessToken = providers.gradleProperty("MODRINTH_TOKEN")
        minecraftVersions = modVersions[minecraftVersion]
        changelog = changelogText
        requires("fabric-api", "fabric-language-kotlin")
        embeds("placeholder-api")
        optional("luckperms")
    }

    /*github {
        repository = "test/example"
        accessToken = providers.gradleProperty("GITHUB_TOKEN")
        commitish = "main"
    }*/

    if (minecraftVersion == latestVersion) {
        discord {
            content = """
                # AutoWhitelist | $projectVersionName
                
                ${changelogText}
            """.trimIndent()

            avatarUrl = "https://cdn.discordapp.com/avatars/1268055578073108574/73106a33f497ea5f2c676bcfb4816917.webp"
            username = "Mod updates"
            webhookUrl = providers.gradleProperty("DISCORD_WEBHOOK")
            dryRunWebhookUrl = providers.gradleProperty("DRY_WEBHOOK")
            style {
                look = "MODERN"
                link = "BUTTON"
                thumbnailUrl = "https://cdn.modrinth.com/data/BMaqFQAd/116458c672aadeb31856563eaff8ed7edd764753.png"
                color = "modrinth"
            }
        }
    }
}
