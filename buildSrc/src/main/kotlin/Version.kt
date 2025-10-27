import kotlinx.serialization.Serializable

@Serializable
class Version(val predicate: String, val properties: Map<String, String>)