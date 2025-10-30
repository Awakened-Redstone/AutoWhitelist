package com.awakenedredstone.multiversion.meta

import com.awakenedredstone.multiversion.game.GameVersion
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.groovy.json.internal.LazyMap
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.problems.internal.impl.logger
import java.net.URI
import java.util.LinkedList
import kotlin.collections.get
import kotlin.collections.iterator

@OptIn(ExperimentalSerializationApi::class)
open class ProjectMeta(
    private val project: Project,
) {
    private val stonecutter: StonecutterBuildExtension = project.extensions.getByName("stonecutter") as StonecutterBuildExtension
    private val meta = Json.decodeFromString<Meta>(project.rootProject.file("project.meta.json").readText())
    val properties: MutableMap<String, Any> = HashMap<String, Any>()
    val propertyMacros: Macros = Macros()
    val propertyTemplates: Macros = Macros()

    init {
        propertyMacros.register("modrinth") { params ->
            if (params.isEmpty()) throw MissingValueException("No modrinth slug or id was provided")
            if (params.size == 1) throw MissingValueException("No loader was provided")
            if (params.size == 2) throw MissingValueException("No game version was provided")
            if (params.size > 3) logger.warn("Too many arguments provided to the macro, ignoring extra arguments")

            val slug = params[0]
            val loader = params[1]
            val gameVersion = params[2]

            logger.debug("Processing modrinth macro (id:$slug,loader:$loader,game_version:$gameVersion)")

            val slurper = JsonSlurper()

            logger.debug("Reading generated version meta file")
            val generatedMeta = project.rootProject.file("project.meta.generated.json")
            if (!generatedMeta.exists()) generatedMeta.writeText("{}")

            val generatedJson = slurper.parse(generatedMeta) as LazyMap
            val versionProperties = generatedJson.get("version_properties") as LazyMap?
            if (versionProperties != null) {
                val props = versionProperties.get("macro:modrinth/$loader/$gameVersion") as LazyMap?
                if (props != null) {
                    logger.debug("Version found in cache, using it")
                    val version = props.get(slug) as String?
                    if (version != null) return@register version
                }
            }

            logger.debug("Getting mod data from modrinth")
            val modrinthResponse = slurper.parse(URI("https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22$loader%22%5D&game_versions=%5B%22$gameVersion%22%5D").toURL()) as List<*>
            val version = (modrinthResponse.first() as Map<*, *>)["version_number"] as String

            generatedJson.compute("version_properties") { key, versionProps ->
                val map = versionProps as LazyMap? ?: LazyMap()
                map.compute("macro:modrinth/$loader/$gameVersion") { _, macroVals ->
                    val map = macroVals as LazyMap? ?: LazyMap()
                    map.put(slug, version)
                    return@compute map
                }

                return@compute map
            }

            logger.debug("Writing generated version meta file")
            generatedMeta.writeText(JsonOutput.toJson(generatedJson))

            return@register version
        }

        propertyTemplates.register("version") { stonecutter.current.version }
        propertyTemplates.register("property") { params ->
            if (params.isEmpty()) throw MissingValueException("No property was provided!")
            project.property(params.joinToString("/")).toString()
        }

        logger.debug("Applying project meta properties")
        if (meta.properties != null) {
            for (property in meta.properties) {
                project.setProperty(property.key, parseProperty(property.value))
            }
        }

        logger.debug("Applying project meta version properties")
        if (meta.versions != null) {
            val versions = LinkedList(meta.versions.entries)
            versions.sortWith { entry1, entry2 ->
                stonecutter.compare(entry1.key, entry2.key)
            }

            val currentVersion = meta.versions[stonecutter.current.version]!!

            if (currentVersion.properties != null) {
                for (property in currentVersion.properties) {
                    project.setProperty(property.key, parseProperty(property.value))
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
                var gameVersionEndIndex: Int = 0

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
    }

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
        val template = property.substring(openIndex + 2, closeIndex)

        val split = template.split(":", limit = 2)
        val processed = propertyTemplates.process(split[0], split.getOrNull(1) ?: "");

        return replaceTemplates(property.replaceRange(openIndex, closeIndex + 1, processed))
    }

    fun applyMacros(property: String): String {
        if (!property.startsWith('#')) return property

        val split = property.split(' ', limit = 2)
        val macro = split[0].substring(1)
        val params = split.getOrNull(1) ?: ""

        return propertyMacros.process(macro, params)
    }
}
