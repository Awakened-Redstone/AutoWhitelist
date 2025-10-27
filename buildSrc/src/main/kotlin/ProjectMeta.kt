import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import kotlinx.serialization.json.Json
import org.apache.groovy.json.internal.LazyMap
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.problems.internal.impl.logger
import java.net.URI
import java.util.Comparator
import java.util.LinkedList

open class ProjectMeta(
    private val project: Project,
    private val stonecutter: StonecutterBuildExtension = project.extensions.getByName("stonecutter") as StonecutterBuildExtension,
) {
    private val meta = Json.decodeFromString<Meta>(project.rootProject.file("project.meta.json").readText())
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

            val slurper = JsonSlurper()

            val generatedMeta = project.rootProject.file("project.meta.generated.json")
            if (!generatedMeta.exists()) generatedMeta.writeText("{}")

            val generatedJson = slurper.parse(generatedMeta) as LazyMap
            val versionProperties = generatedJson.get("version_properties") as LazyMap?
            if (versionProperties != null) {
                val props = versionProperties.get("macro:modrinth/$loader/$gameVersion") as LazyMap?
                if (props != null) {
                    val version = props.get(slug) as String?
                    if (version != null) return@register version
                }
            }

            val modrinthResponse =
                slurper.parse(URI("https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22$loader%22%5D&game_versions=%5B%22$gameVersion%22%5D").toURL()) as List<*>
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

            generatedMeta.writeText(JsonOutput.toJson(generatedJson))

            return@register version
        }

        propertyTemplates.register("version") { stonecutter.current.version }
        propertyTemplates.register("property") { params ->
            if (params.isEmpty()) throw MissingValueException("No property was provided!")
            project.property(params.joinToString("/")).toString()
        }

        logger.quiet("Properties")
        if (meta.properties != null) {
            for (property in meta.properties) {
                project.setProperty(property.key, parseProperty(property.value))
            }
        }

        logger.quiet("Versions")
        if (meta.versions != null) {
            val versions = LinkedList(meta.versions.entries)
            versions.sortWith { entry1, entry2 ->
                stonecutter.compare(entry1.key, entry2.key)
            }

            val versionIndex = meta.versions.keys.indexOf(stonecutter.current.version)
            for (i in versionIndex downTo 1) versions.pop()
            println(versions.peek())
        }
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