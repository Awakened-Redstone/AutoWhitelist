import org.gradle.api.Project
import java.util.WeakHashMap

private val metaCache = WeakHashMap<Project, ProjectMeta>()

val Project.meta: ProjectMeta get() = metaCache.getOrPut(this) { ProjectMeta(this) }