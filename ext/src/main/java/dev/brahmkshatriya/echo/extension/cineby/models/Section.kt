package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Section (
    val name: String,
    val movies: List<Movie>
) {

    @Serializable
    data class Movie(
        val id: Long,
        val image: String,
        val poster: String,
        val description: String,

        @SerialName("original_language")
        val originalLanguage: String,

        val slug: String,
        val title: String,

        @SerialName("genre_ids")
        val genreIDS: List<Long>,

        val rating: Double,

        @SerialName("release_date")
        val releaseDate: String,

        val mediaType: String
    )
}