package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Home(
    val pageProps: PageProps
) {

    @Serializable
    data class PageProps(
        val logos: List<String>,
        val genreSections: List<Section>,
        val trendingSections: List<Section>,
        val defaultSections: List<Section>,
        val streamingSections: List<Section>,
        val messages: JsonObject,
    )
}

