package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Server(
    val sources: List<Source>
) {
    @Serializable
    data class Source(
        val url: JsonElement,
        val quality: String? = null
    )
}