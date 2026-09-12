import com.awakenedredstone.multiversion.values.Repo
import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.configuration.providers.BundleMetadata
import net.fabricmc.loom.configuration.providers.minecraft.library.Library
import net.fabricmc.loom.configuration.providers.minecraft.library.MinecraftLibraryHelper
import net.fabricmc.loom.task.RemapJarTask

plugins {
    // Multiversion applies the right loom version for the current game version
    id("com.awakenedredstone.commons")
    id("com.awakenedredstone.multiversion")
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.9.+"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
    alias(ft.plugins.default)
}

val changelogText: String = if (file("CHANGELOG.md").exists()) {
    file("CHANGELOG.md").readText()
} else {
    "No changelog provided"
}
val minecraftVersion: String = stonecutter.current.version
val latestVersion: String = stonecutter.versions.last().version

var javaVer = meta.property<String>("jvm")

@Suppress("UNCHECKED_CAST")
val modVersions: List<String> = meta.property("versions") as List<String>
val modVersion: String = property("mod_version").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

var archivesBaseName: String = property("archives_base_name").toString()
version = "$modVersion+$minecraftVersion"
group = property("maven_group") as String

@Override
fun file(path: String): File {
    return rootProject.file(path)
}

@Override
fun fileTree(path: String): ConfigurableFileTree {
    return rootProject.fileTree(path)
}

@Suppress("UnstableApiUsage")
val serverLibraries: List<Library>
    get() {
        val loom = project.loom
        if (loom !is LoomGradleExtension) throw AssertionError("Failed to get access to internal loom API")

        val bundleMetadata: BundleMetadata = loom.minecraftProvider.serverBundleMetadata ?: throw NullPointerException("Server bundle metadata can not be null")
        return MinecraftLibraryHelper.getServerLibraries(bundleMetadata)
    }

val includeTransitive: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = true
}

configurations.getByName("include") {
    dependencies.addAllLater(provider {
        val dependencies: MutableList<Dependency> = ArrayList()
        val libraries = serverLibraries

        for (artifact in includeTransitive.incoming.artifacts) {
            val identifier = artifact.id.componentIdentifier

            if (identifier !is ModuleComponentIdentifier) {
                logger.warn("Artifact id for {} is not a ModuleComponentIdentifier", identifier)
                continue
            }

            if (libraries.any { it.group == identifier.group && it.name == identifier.module }) continue
            dependencies.add(project.dependencies.create(identifier.displayName))
        }

        return@provider dependencies
    })
}

fun DependencyHandlerScope.applyMappings() {
    if (!minecraftVersion.startsWith("1.")) return

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        val parchment = meta.property<Repo>("parchment_version")
        parchment("${parchment.repo}:${parchment.version}@zip")
        if (hasProperty("mappings_version")) {
            mappings("dev.lambdaurora:yalmm-mojbackward:${property("mappings_version")}")
        }
    })
}

repositories {
    mavenCentral()
    maven("https://maven.nucleoid.xyz/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.gegy.dev/releases")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    applyMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // Mod dependencies
    include(modApi("me.lucko:fabric-permissions-api:${property("permission_api_version")}")!!)
    include(modApi("xyz.nucleoid:server-translations-api:${property("translation_api_version")}")!!)
    include(modApi("eu.pb4:placeholder-api:${property("placeholder_api_version")}")!!)

    // Libraries
    includeTransitive(api("com.discord4j:discord4j-core:${property("discord4j_version")}") {
        // We don't need this
        exclude("io.netty", "netty-codec-natives-quic")
        exclude("com.google.crypto.tink", "tink")
        exclude("com.google.errorprone", "error_prone_annotations")
        exclude("com.austinv11.servicer", "Servicer")
        exclude("org.checkerframework", "checker-qual")
        exclude("moe.kyokobot.libdave")
    })

    // Include JSpecify on older versions as annotation retention is at runtime
    if (stonecutter.eval(stonecutter.current.version, "<1.21.11")) {
        include(api("org.jspecify:jspecify:1.0.0")!!)
    }

    // Compile only
    compileOnly("net.luckperms:api:5.4")
    compileOnly("dev.gegy:player-roles:1.6.12")
    compileOnly("dev.gegy:player-roles-api:1.6.12")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    compileOnly(annotationProcessor("com.awakenedredstone:prechecks:0.1.0")!!)
}

loom {
    serverOnlyMinecraftJar()
    accessWidenerPath = sc.process(file("src/main/resources/autowhitelist.classtweaker"), "build/processed.classtweaker")

    runConfigs.getByName("server") {
        generateRunConfig = true
        runDirectory = project.rootDir.resolve("run")
    }
}

stonecutter {
    fun registerMacro(name: String, predicate: String, then: String, `else`: String) {
        swaps[name] = when {
            eval(current.version, predicate) -> then
            else -> `else`
        }
    }

    // Stonecutter recommends using snake_case as the naming convention, but I'm using something based of Java's conventions
    // Tt should reflect the case format for what it targets, classes are PascalCase, fields, primitives and methods are camelCase, etc
    registerMacro("WhitelistProfile", ">=1.21.9", "net.minecraft.server.players.NameAndId", "com.mojang.authlib.GameProfile")
    registerMacro("AuthlibNameAndId", "<26.3-rc-1", "com.mojang.authlib.yggdrasil.response.NameAndId", "com.mojang.authlib.services.response.NameAndId")
    registerMacro("AuthlibHttpService", "<26.3-rc-1", "com.mojang.authlib.HttpAuthenticationService", "com.mojang.authlib.HttpDiscoveryService")
    registerMacro("entryPatchReturn", ">=1.21.9", "boolean", "void")
}

fletchingTable {
    relocate.configure(sourceSets.main) {
        matching("(*.mixins).json5") {
            into("$1.json")
            with(Json5ToJson)
        }
    }

    lang.configure(sourceSets.main) {
        filters.setIncludes(listOf("data/autowhitelist/lang/*.yml"))
        jsonIndent = "  "
    }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "build"
        dependsOn(tasks.named("build"))
    }

    rootProject.tasks.register("cleanActive") {
        group = "build"
        dependsOn(tasks.named("build"))
    }
}

tasks {
    processResources {
        val map = mapOf(
            "version" to version,
            "loader" to meta.property("loader_version"),
            "fabric_api" to meta.property("fabric_api_version"),
            "placeholder_api" to meta.property("placeholder_api_version"),
            "translation_api" to meta.property("translation_api_version"),
            "minecraft" to meta.property("predicate")
        )

        inputs.properties(map)

        filesMatching("fabric.mod.json") {
            expand(map)
        }
    }

    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    compileJava {
        sourceCompatibility = "25" // TODO: Java 21?
        targetCompatibility = javaVer
        options.encoding = "UTF-8"
    }

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task if it is present.
    // If you remove this task, sources will not be generated.
    register<Jar>("sourcesJar") {
        dependsOn("classes")
        archiveClassifier = "sources"
        from(sourceSets["main"].allSource)
    }

    if (meta.hasRemapping) {
        register("prepareRemapJar") {
            dependsOn(named("shadowJar"))
        }

        named<RemapJarTask>("remapJar") {
            dependsOn(named("prepareRemapJar"))
            inputFile.set(jar.get().archiveFile.get().asFile)
        }

        register<RemapJarTask>("remapMavenJar") {
            dependsOn(jar)
            inputFile.set(jar.get().archiveFile)
            archiveFileName.set("${archivesBaseName}-${version}-maven.jar")
            addNestedDependencies.set(false)
            java.withSourcesJar()
        }

        build.get().dependsOn(getByName("remapMavenJar"))
    }
}

fun getJarTask(): AbstractArchiveTask {
    return tasks.getByName(if (meta.hasRemapping) "remapJar" else "jar") as AbstractArchiveTask
}

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

fun <T> action(action: Action<T>): Action<T> where T : Task {
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

// TODO: Allow publish to continue after failing a step
publishMods {
    file = getJarTask().archiveFile
    changelog = changelogText
    type = projectVersionType
    modLoaders.add("fabric")
    displayName = "[$minecraftVersion] $projectVersionName"

    modrinth {
        projectId = "BMaqFQAd"
        accessToken = providers.gradleProperty("MODRINTH_TOKEN")
        minecraftVersions = modVersions
        changelog = changelogText
        requires("fabric-api")
        embeds("placeholder-api")
        optional("luckperms")
        optional("player-roles")
    }

    curseforge {
        projectId = "575422"
        projectSlug = "autowhitelist" // Required for discord webhook
        accessToken = providers.gradleProperty("CURSEFORGE_TOKEN")
        minecraftVersions = modVersions
        changelog = changelogText
        requires("fabric-api")
        embeds("text-placeholder-api")
        optional("luckperms")
        optional("player-roles")
    }

    if (minecraftVersion == latestVersion) {
        discord {
            webhookUrl = providers.gradleProperty("DISCORD_WEBHOOK")
            dryRunWebhookUrl = providers.gradleProperty("DRY_WEBHOOK")

            username = "Mod updates"
            avatarUrl = "https://cdn.discordapp.com/avatars/1268055578073108574/73106a33f497ea5f2c676bcfb4816917.webp"

            content = """
                # AutoWhitelist | $projectVersionName
                
                ${if (changelogText.length > 4000) "Changelog is too long, check it on [Modrinth](https://modrinth.com/mod/autowhitelist/changelog)" else changelogText}
            """.trimIndent()

            style {
                look = "MODERN"
                link = "BUTTON"
                thumbnailUrl = "https://cdn.modrinth.com/data/BMaqFQAd/116458c672aadeb31856563eaff8ed7edd764753.png"
                color = "modrinth"
            }

            /*message {
                componentsV2 = true;
                components {}
            }*/
        }
    }
}
