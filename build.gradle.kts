import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.kikugie.semver.data.Version
import me.modmuss50.mpp.ReleaseType

plugins {
    id("fabric-loom") version "1.12+"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
    id("com.gradleup.shadow") version "8.+"
    id("com.awakenedredstone.multiversion")
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

var javaVer = "21"

@Suppress("UNCHECKED_CAST")
val modVersions: List<String> = meta.property("versions") as List<String>
val modVersion: String = property("mod_version").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

var archivesBaseName: String = property("archives_base_name").toString()
version = "$modVersion+$minecraftVersion"
group = property("maven_group") as String

configurations.configureEach {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${property("loader_version")}")
    }
}

@Override
fun file(path: String): File {
    return rootProject.file(path)
}

@Override
fun fileTree(path: String): ConfigurableFileTree {
    return rootProject.fileTree(path)
}

val accessWidener = findAccessWidener()

fun findAccessWidenerFile(): File {
    return file("src/main/resources/accesswideners/${accessWidener.second}")
}

fun findAccessWidener(): Pair<String, String> {
    val wideners = fileTree("src/main/resources/accesswideners")
    val versions: MutableSet<Version> = sortedSetOf();
    val sampleFileName = wideners.first().name
    val filePrefix = sampleFileName.substringBefore('.')
    val fileSuffix = sampleFileName.substringAfterLast('.')

    wideners.visit {
        val version = file.name.substringAfter('.').substringBeforeLast('.')
        versions += sc.parse(version)
    }

    var returnValue: Pair<String, String>? = null;
    for (version in versions.reversed()) {
        if (sc.eval(sc.current.version, ">=${version.value}")) {
            returnValue = Pair(version.value, "$filePrefix.${version.value}.$fileSuffix")
            break
        }
    }

    if (returnValue == null) {
        throw MissingResourceException("No valid access widener for ${sc.current.version} found!")
    }

    logger.info("Excluding for $minecraftVersion")
    for (version in versions) {
        if (version.value != returnValue.first) {
            logger.info("Excluding: ${version.value}")
            tasks.processResources.get().exclude("**/$filePrefix.${version.value}.$fileSuffix")
        }
    }

    return returnValue
}

val shade: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // Mod dependencies
    include(modImplementation("eu.pb4:placeholder-api:${property("placeholder_api_version")}")!!)
    include(modImplementation("xyz.nucleoid:server-translations-api:${property("translation_api_version")}")!!)
    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:${property("mixinextras")}")!!)!!)

    // Upgrade jackson on older MC versions
    include(implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")!!)
    include(implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")!!)

    // Libraries
    include(api("blue.endless:jankson:${property("jankson_version")}")!!)

    api("com.discord4j:discord4j-core:${property("discord4j_version")}")
    shade("com.discord4j:discord4j-core:${property("discord4j_version")}")

    // Runtime only
    modRuntimeOnly("net.fabricmc:fabric-language-kotlin:${property("kotlin_version")}")

    // Compile only
    modCompileOnly("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
}

loom {
    accessWidenerPath = findAccessWidenerFile()

    runConfigs.getByName("server") {
        ideConfigGenerated(true)
        runDir("../../run")
    }

    runConfigs.getByName("client") {
        ideConfigGenerated(false)
        runDir("../../run")
    }
}

stonecutter {
    fun registerMacro(name: String, predicate: String, then: String, `else`: String) {
        swaps[name] = when {
            eval(current.version, predicate) -> then
            else -> `else`
        }
    }

    registerMacro("WhitelistProfile", ">=1.21.9", "net.minecraft.server.PlayerConfigEntry", "com.mojang.authlib.GameProfile")
    registerMacro("entryPatchReturn", ">=1.21.9", "boolean", "void")
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "build"
        dependsOn(tasks.named("build"))
    }
}

tasks.compileJava {
    sourceCompatibility = "21"
    targetCompatibility = javaVer
    options.encoding = "UTF-8"
}

tasks.processResources {
    val map = mapOf(
        "version" to version,
        "minecraft" to meta.property("predicate"),
        "accesswidener" to accessWidener.second
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
    destinationDirectory = file("${layout.buildDirectory.asFile.get().absolutePath}/tmp/thinJar")
    /*from("LICENSE") {
        rename { "${it}_${archivesBaseName}" }
    }*/
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    destinationDirectory = file("${layout.buildDirectory.file("tmp/shadowJar").get().asFile.absolutePath}")
    relocate("net.dv8tion.jda", "com.awakenedredstone.autowhitelist.lib.jda")
    relocate("com.jagrosh.jdautilities", "com.awakenedredstone.autowhitelist.lib.jdautils")
    relocate("pw.chew.jdachewtils", "com.awakenedredstone.autowhitelist.lib.chewtils")
    relocate("com.discord4j", "com.awakenedredstone.autowhitelist.lib.discord4j")
    exclude("META-INF/maven/**/*", "META-INF/*.txt", "META-INF/proguard/*", "META-INF/LICENSE")
    from("LICENSE") {
        rename { "${it}_${archivesBaseName}" }
    }
}

tasks.register("prepareRemapJar") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    dependsOn(tasks.named("prepareRemapJar"))
    inputFile.set(tasks.named<ShadowJar>("shadowJar").get().archiveFile.get().asFile)
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
    if (changelogText.isEmpty()) {
        throw MissingResourceException("Update the changelog!")
    }
}

tasks.getByName("modrinth").doFirst(checks)
tasks.getByName("modrinthSyncBody").doFirst(checks)
tasks.getByName("publishMods").doFirst(checks)

modrinth {
    token = providers.gradleProperty("MODRINTH_TOKEN")
    projectId = "BMaqFQAd"
    syncBodyFrom = file("README.md").readText()
}

publishMods {
    file = (tasks.getByName("remapJar") as AbstractArchiveTask).archiveFile
    changelog = changelogText
    type = projectVersionType
    modLoaders.add("fabric")
    displayName = "[$minecraftVersion] $projectVersionName"

    modrinth {
        projectId = "BMaqFQAd"
        accessToken = providers.gradleProperty("MODRINTH_TOKEN")
        minecraftVersions = modVersions
        changelog = changelogText
        requires("fabric-api", "fabric-language-kotlin")
        embeds("placeholder-api")
        optional("luckperms")
    }

    curseforge {
        projectId = "575422"
        projectSlug = "autowhitelist" // Required for discord webhook
        accessToken = providers.gradleProperty("CURSEFORGE_TOKEN")
        minecraftVersions = modVersions
        changelog = changelogText
        requires("fabric-api", "fabric-language-kotlin")
        embeds("text-placeholder-api")
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
                
                $changelogText
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
