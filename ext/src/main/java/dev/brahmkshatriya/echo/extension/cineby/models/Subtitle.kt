package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.Serializable

@Serializable
data class Subtitle(
    val id: String? = null,
    val url: String? = null,
    val format: String? = null,
    val encoding: String? = null,
    val display: String? = null,
    val language: String? = null,
    val media: String? = null,
    val isHearingImpaired: Boolean? = null,
    val source: String? = null
)