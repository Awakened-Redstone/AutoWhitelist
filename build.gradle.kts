import com.modrinth.minotaur.dependencies.ModDependency
import groovy.json.JsonSlurper
import java.io.File

import com.google.common.collect.Ordering
import java.util.*

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
val modVersions: Map<String, List<String>> = JsonSlurper().parse(file("modrinth.json")) as Map<String, List<String>>
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
    if (isOrOlder("1.18.2")) {
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
    include("com.fasterxml.jackson.core:jackson-annotations:2.16.0")
    include("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    include("com.fasterxml.jackson.core:jackson-core:2.16.0")
    include("com.neovisionaries:nv-websocket-client:2.14")
    include("org.apache.commons:commons-collections4:4.4")
    include("com.squareup.okhttp3:okhttp:4.12.0")
    include("com.squareup.okio:okio-jvm:2.13.0")
    include("net.sf.trove4j:trove4j:3.0.3")

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
    val minecraftMajor = minecraftVersion.split(Regex("\\."), 3).take(2).joinToString(".")
    val map = mapOf(
        "version" to version,
        "minecraft_major" to minecraftMajor
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
    val projectVersion: String = project.version.toString()
    val projectVersionNumber: List<String> = projectVersion.split(Regex("-"), 2)
    var projectVersionName: String = "Release ${projectVersionNumber[0]}"
    if (projectVersion.contains("beta")) {
        val projectBeta: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Beta ${projectBeta[1]}"
        versionType = "beta"
    } else if (projectVersion.contains("alpha")) {
        val projectAlpha: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Alpha ${projectAlpha[1]}"
        versionType = "alpha"
    } else if (projectVersion.contains("rc")) {
        val projectRC: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Release Candidate ${projectRC[1]}"
        versionType = "beta"
    }

    if (modVersions[minecraftVersion] == null) {
        throw Throwable("Please update modrinth.json")
    }

    token = System.getenv("MODRINTH_TOKEN")
    projectId = "BMaqFQAd"
    versionName = projectVersionName
    changelog = CHANGELOG
    uploadFile = tasks.getByName("remapJar")
    gameVersions = modVersions[minecraftVersion]
    syncBodyFrom = file("README.md").readText()
    dependencies = listOf(
        ModDependency("fabric-language-kotlin", "required"),
        ModDependency("luckperms", "optional"),
        ModDependency("placeholder-api", "embedded")
    )
}

//region Semver classes
/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class VersionNumber(
    val major: Int,
    val minor: Int,
    val micro: Int,
    val patch: Int = 0,
    val qualifier: String? = null
) : Comparable<VersionNumber> {
    companion object {
        val UNKNOWN = version(0)
        private val DEFAULT_SCHEME = DefaultScheme()
        private val PATCH_SCHEME = SchemeWithPatchVersion()

        fun version(major: Int) = version(major, 0)
        fun version(major: Int, minor: Int) = VersionNumber(major, minor, 0)

        fun scheme(): Scheme = DEFAULT_SCHEME
        fun withPatchNumber(): Scheme = PATCH_SCHEME
        fun parse(versionString: String): VersionNumber = DEFAULT_SCHEME.parse(versionString)
    }

    private val scheme = if (patch == 0) DEFAULT_SCHEME else PATCH_SCHEME

    override fun compareTo(other: VersionNumber): Int {
        if (major != other.major) {
            return major - other.major
        }
        if (minor != other.minor) {
            return minor - other.minor
        }
        if (micro != other.micro) {
            return micro - other.micro
        }
        if (patch != other.patch) {
            return patch - other.patch
        }

        return Ordering.natural<String?>().nullsLast<String?>().compare(toLowerCase(qualifier), toLowerCase(other.qualifier))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionNumber

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (micro != other.micro) return false
        if (patch != other.patch) return false
        if (qualifier != other.qualifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + micro
        result = 31 * result + patch
        result = 31 * result + (qualifier?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return scheme.format(this)
    }

    fun getBaseVersion(): VersionNumber {
        return VersionNumber(major, minor, micro, patch, null)
    }

    private fun toLowerCase(string: String?): String? {
        return string?.lowercase(Locale.getDefault())
    }

    interface Scheme {
        fun parse(value: String): VersionNumber
        fun format(versionNumber: VersionNumber): String
    }

    private abstract class AbstractScheme : Scheme {
        abstract val depth: Int

        override fun parse(value: String): VersionNumber {
            if (value.isEmpty()) {
                return UNKNOWN
            }
            val scanner = Scanner(value)
            var minor = 0
            var micro = 0
            var patch = 0

            if (!scanner.hasDigit()) {
                return UNKNOWN
            }
            val major: Int = scanner.scanDigit()
            if (scanner.isSeparatorAndDigit('.')) {
                scanner.skipSeparator()
                minor = scanner.scanDigit()
                if (scanner.isSeparatorAndDigit('.')) {
                    scanner.skipSeparator()
                    micro = scanner.scanDigit()
                    if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
                        scanner.skipSeparator()
                        patch = scanner.scanDigit()
                    }
                }
            }

            if (scanner.isEnd()) {
                return VersionNumber(major, minor, micro, patch, null)
            }

            if (scanner.isQualifier()) {
                scanner.skipSeparator()
                return VersionNumber(major, minor, micro, patch, scanner.remainder())
            }

            return UNKNOWN
        }

        private class Scanner(versionString: String) {
            var pos = 0
            val str = versionString

            fun hasDigit(): Boolean {
                return pos < str.length && Character.isDigit(str.get(pos))
            }

            fun isSeparatorAndDigit(vararg separators: Char): Boolean {
                return pos < str.length - 1 && oneOf(*separators) && Character.isDigit(str.get(pos + 1))
            }

            fun isSeparatorAndDigit(separators: String): Boolean {
                return isSeparatorAndDigit(*separators.toCharArray())
            }

            private fun oneOf(vararg separators: Char): Boolean {
                val current = str.get(pos)
                separators.forEach {
                    if (current == it) {
                        return true
                    }
                }
                return false
            }

            fun isQualifier(): Boolean {
                return pos < str.length - 1 && oneOf('.', '-')
            }

            fun scanDigit(): Int {
                val start = pos
                while (hasDigit()) {
                    pos++
                }
                return str.substring(start, pos).toInt()
            }

            fun isEnd(): Boolean {
                return pos == str.length
            }

            fun skipSeparator() {
                pos++
            }

            fun remainder(): String? {
                return if (pos == str.length) null else str.substring(pos)
            }
        }
    }

    private class DefaultScheme : AbstractScheme() {
        override val depth = 3
        override fun format(versionNumber: VersionNumber): String {
            return String.format(
                "%d.%d.%d%s",
                versionNumber.major,
                versionNumber.minor,
                versionNumber.micro,
                if (versionNumber.qualifier != null) "-${versionNumber.qualifier}" else ""
            )
        }
    }

    private class SchemeWithPatchVersion : AbstractScheme() {
        override val depth = 4
        override fun format(versionNumber: VersionNumber): String {
            return String.format(
                "%d.%d.%d.%d%s",
                versionNumber.major,
                versionNumber.minor,
                versionNumber.micro,
                versionNumber.patch,
                if (versionNumber.qualifier != null) "-${versionNumber.qualifier}" else ""
            )
        }
    }
}
//endregion
