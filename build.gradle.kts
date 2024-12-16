import com.modrinth.minotaur.dependencies.ModDependency
import groovy.json.JsonSlurper
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.discord.MessageLook
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

val CHANGELOG: String =
    if (file("CHANGELOG.md").exists()) {
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
        exclude(module = "okio-jvm")
    })

    // JDA dependencies
    include("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    include("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    include("com.fasterxml.jackson.core:jackson-core:2.17.0")
    include("com.neovisionaries:nv-websocket-client:2.14")
    include("org.apache.commons:commons-collections4:4.4")
    include("com.squareup.okhttp3:okhttp:4.12.0")
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
    val versions = JsonSlurper().parse(file("versions/versions.json")) as Map<*, *>
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

modrinth {
    if (modVersions[minecraftVersion] == null) {
        throw Throwable("Please update modrinth.json, missing $minecraftVersion")
    }

    versionType = projectVersionType.toString().lowercase()
    token = System.getenv("MODRINTH_TOKEN")
    projectId = "BMaqFQAd"
    versionName = "[$minecraftVersion] $projectVersionName"
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

publishMods {
    if (modVersions[minecraftVersion] == null) {
        throw Throwable("Please update modrinth.json, missing $minecraftVersion")
    }

    file = (tasks.getByName("remapJar") as AbstractArchiveTask).archiveFile
    changelog = CHANGELOG
    type = projectVersionType
    modLoaders.add("fabric")
    displayName = projectVersionName

    /*curseforge {
        displayName = projectVersionName
        projectId = "1099230"
        projectSlug = "default-components" // Required for discord webhook
        accessToken = providers.gradleProperty("CURSEFORGE_TOKEN")
        minecraftVersions = modVersions[minecraftVersion]
        changelog = CHANGELOG
        requires("fabric-api", "fabric-language-kotlin")
        embeds("placeholder-api")
        optional("luckperms")
    }*/

    modrinth {
        displayName = "[$minecraftVersion] $projectVersionName"
        projectId = "BMaqFQAd"
        accessToken = providers.gradleProperty("MODRINTH_TOKEN")
        minecraftVersions = modVersions[minecraftVersion]
        changelog = CHANGELOG
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
                
                $CHANGELOG
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
