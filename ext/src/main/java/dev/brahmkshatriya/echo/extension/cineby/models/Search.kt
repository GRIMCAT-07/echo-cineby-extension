package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.Serializable

@Serializable
data class Search(
    val pageProps: PageProps
) {
    @Serializable
    data class PageProps(
        val trending: List<Section.Movie>,
    )
}