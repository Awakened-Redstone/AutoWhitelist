package com.awakenedredstone.multiversion.meta

import com.awakenedredstone.multiversion.fetch.fabricmeta.game.GameVersion
import com.awakenedredstone.multiversion.fetch.fabricmeta.yarn.YarnVersion
import com.awakenedredstone.multiversion.fetch.modrinth.ProjectVersion
import com.awakenedredstone.multiversion.putIfAbsentAndGet
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.problems.internal.impl.logger
import java.io.File
import java.net.URI
import java.util.*


@OptIn(ExperimentalSerializationApi::class)
open class ProjectMeta(private val project: Project) {
    private val stonecutter: StonecutterBuildExtension = project.extensions.getByName("stonecutter") as StonecutterBuildExtension
    private val meta = Json.decodeFromString<Meta>(project.rootProject.file("project.meta.json").readText())
    private val json = jacksonObjectMapper()
    private val generatedMetaFile: File = project.rootProject.file("project.meta.generated.json")
    private val generatedMeta: ObjectNode = readObjectJsonFile(generatedMetaFile)
    val properties: MutableMap<String, Any> = HashMap<String, Any>()
    val propertyMacros: Macros = Macros()
    val propertyTemplates: Macros = Macros()

    init {
        registerDefaultMacros()
        registerDefaultTemplates()

        val propertiesToApply = mutableMapOf<String, String>()
        if (meta.properties != null) {
            for (property in meta.properties) {
                propertiesToApply.put(property.key, property.value)
            }
        }

        if (meta.versions != null) {
            logger.debug("Processing project version meta")
            val versions = LinkedList(meta.versions.entries)
            versions.sortWith { entry1, entry2 ->
                stonecutter.compare(entry1.key, entry2.key)
            }

            val currentVersion = meta.versions[stonecutter.current.version]!!

            if (currentVersion.properties != null) {
                for (property in currentVersion.properties) {
                    propertiesToApply.put(property.key, property.value)
                }
            }

            var predicate = ""
            var versionRange: List<String> = listOf()

            logger.debug("Getting version meta from FabricMC")
            val gameVersions = URI("https://meta.fabricmc.net/v2/versions/game").toURL().openStream().use {
                Json.decodeFromStream<List<GameVersion>>(it).filter { (version, stable) -> stable }.map { version -> version.version }
            }

            if (currentVersion.predicate == "auto") {
                logger.debug("Creating predicate from context")
                val versionIndex = meta.versions.keys.indexOf(stonecutter.current.version)
                repeat(versionIndex + 1) { versions.pop() }

                val gameVersionIndex = gameVersions.indexOf(stonecutter.current.version) + 1
                var gameVersionEndIndex = 0

                val peek = versions.peek()
                if (peek == null) {
                    val mcSemver = stonecutter.parse(stonecutter.current.version) as SemanticVersion
                    val newSemver = SemanticVersion(intArrayOf(mcSemver.components[0], mcSemver.components[1] + 1))

                    predicate = ">=${stonecutter.current.version} <${newSemver.value}"
                } else {
                    predicate = ">=${stonecutter.current.version} <${peek.key}"
                    gameVersionEndIndex = gameVersions.indexOf(peek.key) + 1
                }

                versionRange = gameVersions.subList(gameVersionEndIndex, gameVersionIndex)
            } else {
                predicate = currentVersion.predicate
                versionRange = gameVersions.filter { version -> stonecutter.eval(version, predicate) }
            }

            logger.debug("Version: {}", stonecutter.current.version)
            logger.debug("Predicate: {}", predicate)
            logger.debug("Version list: {}", versionRange)

            properties.put("predicate", predicate)
            properties.put("versions", versionRange)
        }

        logger.debug("Applying project meta properties")
        for ((name, value) in propertiesToApply) {
            project.setProperty(name, parseProperty(value))
        }

        logger.debug("Writing generated version meta file")
        json.writeValue(generatedMetaFile, generatedMeta)
    }

    @Suppress("unused")
    inline fun <reified T> property(property: String): T {
        return properties[property] as T
    }

    fun parseProperty(property: String): String {
        return applyMacros(replaceTemplates(property))
    }

    fun replaceTemplates(property: String): String {
        val openIndex = property.indexOf($$"${")
        if (openIndex == -1) return property

        val closeIndex = property.indexOf("}")
        if (closeIndex == -1) throw IllegalStateException("Unclosed template!")

        val template = property.substring(openIndex + 2, closeIndex)

        val split = template.split(":", limit = 2)
        val processed = propertyTemplates.process(split[0], split.getOrNull(1) ?: "")

        return replaceTemplates(property.replaceRange(openIndex, closeIndex + 1, processed))
    }

    fun applyMacros(property: String): String {
        if (!property.startsWith('#')) return property

        val split = property.split(' ', limit = 2)
        val macro = split[0].substring(1)
        val params = split.getOrNull(1) ?: ""

        return propertyMacros.process(macro, params)
    }

    private fun registerDefaultMacros() {
        propertyMacros.register("modrinth", "slug", "loader", "game_version") { (slug, loader, gameVersion) ->
            val versionProperties = generatedMeta.putIfAbsentAndGet("version_properties") { ObjectNode(it) }
            val props = versionProperties.putIfAbsentAndGet("macro:modrinth/$loader/$gameVersion") { ObjectNode(it) }

            if (props.has(slug)) {
                logger.debug("Found $slug version in cache, using it")
                return@register props.get(slug).textValue()
            }

            val modrinthResponse = Macros.fetchData(
                "https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22$loader%22%5D&game_versions=%5B%22$gameVersion%22%5D",
                object : TypeReference<List<ProjectVersion>>() {}
            )

            val version = modrinthResponse.first().versionNumber

            props.set<TextNode>(slug, TextNode(version))

            return@register version
        }

        propertyMacros.register("yarn", "game_version") { (gameVersion) ->
            val versionProperties = generatedMeta.putIfAbsentAndGet("version_properties") { ObjectNode(it) }

            if (versionProperties.has("macro:yarn/$gameVersion")) {
                logger.debug("Found yarn version in cache, using it")
                return@register versionProperties.get("macro:yarn/$gameVersion").intValue().toString()
            }

            val yarnResponse = Macros.fetchData(
                "https://meta.fabricmc.net/v2/versions/yarn/$gameVersion",
                object : TypeReference<List<YarnVersion>>() {}
            )
            val version = yarnResponse.first().build

            versionProperties.set<TextNode>("macro:yarn/$gameVersion", IntNode(version))

            return@register version.toString()
        }
    }

    private fun registerDefaultTemplates() {
        propertyTemplates.register("version") { stonecutter.current.version }
        propertyTemplates.register("property") { params ->
            if (params.isEmpty()) throw MissingValueException("No property was provided!")
            project.property(params.joinToString("/")).toString()
        }
    }

    private fun readObjectJsonFile(file: File): ObjectNode {
        logger.debug("Reading {}", file.name)
        if (!file.exists() || file.readText().isBlank()) file.writeText("{}")

        return json.readValue(file, ObjectNode::class.java)
    }
}
