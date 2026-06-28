import com.awakenedredstone.multiversion.values.Repo
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.kikugie.semver.data.Version
import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.configuration.providers.BundleMetadata
import net.fabricmc.loom.configuration.providers.minecraft.library.Library
import net.fabricmc.loom.configuration.providers.minecraft.library.MinecraftLibraryHelper
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
    id("com.gradleup.shadow") version "9.3.+"
    id("com.awakenedredstone.multiversion")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.23"
    id("com.awakenedredstone.commons")
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

// val classTweaker = findClassTweaker()

/*@Deprecated("Deprecated in favor of stonecutter")
fun findClassTweakerFile(): File {
    return file("src/main/resources/classtweakers/${classTweaker.second}")
}*/

@Deprecated("Deprecated in favor of stonecutter")
fun findClassTweaker(): Pair<String, String> {
    val wideners = fileTree("src/main/resources/classtweakers")
    val versions: MutableSet<Version> = sortedSetOf()
    val sampleFileName = wideners.first().name
    val filePrefix = sampleFileName.substringBefore('.')
    val fileSuffix = sampleFileName.substringAfterLast('.')

    wideners.visit {
        val version = file.name.substringAfter('.').substringBeforeLast('.')
        versions += sc.parse(version)
    }

    var returnValue: Pair<String, String>? = null
    for (version in versions.reversed()) {
        if (sc.eval(sc.current.version, ">=${version.value}")) {
            returnValue = Pair(version.value, "$filePrefix.${version.value}.$fileSuffix")
            break
        }
    }

    if (returnValue == null) {
        throw MissingResourceException("No valid class tweaker for ${sc.current.version} found!")
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

@Suppress("UnstableApiUsage")
val serverLibraries: List<Library>
    get() {
        val loom = project.loom
        if (loom !is LoomGradleExtension) throw AssertionError("Failed to get access to internal loom API")

        val bundleMetadata: BundleMetadata = loom.minecraftProvider.serverBundleMetadata ?: throw NullPointerException("Server bundle metadata can not be null")
        return MinecraftLibraryHelper.getServerLibraries(bundleMetadata)
    }

val shadeApi: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = true

    project.configurations.getByName("api").extendsFrom(this)
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
        logger.lifecycle("===== WAFF =====")

        libraries.map { it.mavenNotation() }.forEach { logger.lifecycle("Library: {}",it ) }
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

// Make sure that we don't use other versions of libraries from Minecraft
/*afterEvaluate {
    for (dependency in configurations.getByName("minecraftServerRuntimeLibraries").incoming.dependencies) {
        logger.lifecycle("Trying {}", dependency)
        shadeApi.exclude(dependency.group, dependency.name)
    }
}*/

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
    include(modApi("eu.pb4:placeholder-api:${property("placeholder_api_version")}")!!)
    include(modApi("xyz.nucleoid:server-translations-api:${property("translation_api_version")}")!!)

    // Libraries
    includeTransitive(api("com.discord4j:discord4j-core:${property("discord4j_version")}") {
        // We don't need this
        exclude("com.google.errorprone", "error_prone_annotations")
        exclude("com.austinv11.servicer", "Servicer")
        exclude("org.checkerframework", "checker-qual")
        exclude("moe.kyokobot.libdave")
    })

    // Include JSpecify on older versions as annotation retention is at runtime
    if (stonecutter.eval(stonecutter.current.version, "<1.21.11")) {
        include(api("org.jspecify:jspecify:1.0.0")!!)
    }

    // Runtime only
    // modRuntimeOnly("net.fabricmc:fabric-language-kotlin:${property("kotlin_version")}")

    // Compile only
    compileOnly("net.luckperms:api:5.4")
    compileOnly("dev.gegy:player-roles:1.6.12")
    compileOnly("dev.gegy:player-roles-api:1.6.12")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    compileOnly(annotationProcessor("com.awakenedredstone:prechecks:0.1.0")!!)
}

loom {
    serverOnlyMinecraftJar()
    accessWidenerPath = file("src/main/resources/autowhitelist.classtweaker")// findClassTweakerFile()

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

    registerMacro("WhitelistProfile", ">=1.21.9", "net.minecraft.server.players.NameAndId", "com.mojang.authlib.GameProfile")
    registerMacro("entryPatchReturn", ">=1.21.9", "boolean", "void")
}

fletchingTable {
    j52j.register("main") {
        extension("json", "*.mixins.json5")
    }

    lang.register("main") {
        patterns.add("data/autowhitelist/lang/*.yml")
        prettyPrint = true
    }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "build"
        dependsOn(tasks.named("build"))
    }
}

tasks {
    processResources {
        val map = mapOf(
            "version" to version,
            "loader" to meta.property("loader_version"),
            "fapi" to meta.property("fabric_api_version"),
            "minecraft" to meta.property("predicate"),
            // "classtweaker" to classTweaker.second
        )

        inputs.properties(map)

        filesMatching("fabric.mod.json") {
            expand(map)
        }
    }

    named<ShadowJar>("shadowJar") {
        //archiveClassifier = null
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(shadeApi)
        relocate("discord4j", "com.awakenedredstone.autowhitelist.lib.discord4j")
        exclude("META-INF/maven/**/*", "META-INF/*.txt", "META-INF/proguard/*", "META-INF/LICENSE", "META-INF/license/*", "META-INF/NOTICE")

        dependencies {
            val libraries = serverLibraries

            // Don't shadow dependencies used by Minecraft, there is no need and can even cause problems
            exclude { resolved ->
                val any = libraries.any { resolved.moduleGroup == it.group && resolved.moduleName == it.name }
                return@exclude any
            }
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

    jar {
        // archiveClassifier = "thin"
        /*from("LICENSE") {
            rename { "${it}_${archivesBaseName}" }
        }*/
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
            inputFile.set(named<ShadowJar>("shadowJar").get().archiveFile.get().asFile)
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
