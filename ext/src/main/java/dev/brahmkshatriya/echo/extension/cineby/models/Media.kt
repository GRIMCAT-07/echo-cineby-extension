package dev.brahmkshatriya.echo.extension.cineby.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val id: Long,
    val adult: Boolean,

    val type: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,

    val name: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,

    val title: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,

    val tagline: String? = null,
    val overview: String? = null,

    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,

    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("last_air_date")
    val lastAirDate: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,

    @SerialName("number_of_episodes")
    val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons")
    val numberOfSeasons: Int? = null,

    val runtime: Long? = null,
    val seasons: List<Season>? = null,

    val similar: Search? = null,
    val recommendations: Search? = null,

    @SerialName("vote_average")
    val voteAverage: Double? = null,
    @SerialName("vote_count")
    val voteCount: Long? = null,
    val popularity: Double? = null,

    val networks: List<Company>? = null,
    @SerialName("production_companies")
    val productionCompanies: List<Company>? = null,

    @SerialName("created_by")
    val createdBy: List<CreatedBy>? = null,

    val genres: List<Genre>? = null,
    @SerialName("genre_ids")
    val genreIDS: List<Long>? = null,

    @SerialName("in_production")
    val inProduction: Boolean? = null,
    val status: String? = null,

    @SerialName("origin_country")
    val originCountry: List<String>? = null,
    @SerialName("imdb_id")
    val imdbID: String? = null,
    @SerialName("original_language")
    val originalLanguage: String? = null,
    val revenue: Long? = null,
    val video: Boolean? = null,
) {

    @Serializable
    data class CreatedBy(
        val id: Long,
        @SerialName("credit_id")
        val creditID: String,
        val name: String,
        @SerialName("original_name")
        val originalName: String? = null,
        val gender: Long? = null,
        @SerialName("profile_path")
        val profilePath: String? = null,
    )

    @Serializable
    data class Genre(
        val id: Long,
        val name: String
    )

    @Serializable
    data class Search(
        val page: Long,
        val results: List<Media>,
        @SerialName("total_pages")
        val totalPages: Long,
        @SerialName("total_results")
        val totalResults: Long
    )


    @Serializable
    data class Season(
        @SerialName("_id")
        val id: String? = null,

        @SerialName("air_date")
        val airDate: String? = null,

        val episodes: List<Episode>? = null,
        val name: String? = null,
        val overview: String? = null,

        @SerialName("id")
        val seasonID: Int? = null,

        @SerialName("poster_path")
        val posterPath: String? = null,

        @SerialName("season_number")
        val seasonNumber: Int,

        @SerialName("vote_average")
        val voteAverage: Double? = null,

        @SerialName("episode_count")
        val episodeCount: Int? = null
    )

    @Serializable
    data class Episode(
        @SerialName("air_date")
        val airDate: String? = null,

        @SerialName("episode_number")
        val episodeNumber: Long? = null,

        @SerialName("episode_type")
        val episodeType: String? = null,

        val id: Long? = null,
        val name: String? = null,
        val overview: String? = null,

        @SerialName("production_code")
        val productionCode: String? = null,

        val runtime: Long? = null,

        @SerialName("season_number")
        val seasonNumber: Long? = null,

        @SerialName("show_id")
        val showID: Long? = null,

        @SerialName("still_path")
        val stillPath: String? = null,

        @SerialName("vote_average")
        val voteAverage: Double? = null,

        @SerialName("vote_count")
        val voteCount: Long? = null,

        val crew: List<Crew>? = null,

        @SerialName("guest_stars")
        val guestStars: List<Crew>? = null
    )

    @Serializable
    data class Crew(
        val job: String? = null,
        val department: String? = null,

        @SerialName("credit_id")
        val creditID: String? = null,

        val adult: Boolean? = null,
        val gender: Long? = null,
        val id: Long? = null,

        @SerialName("known_for_department")
        val knownForDepartment: String? = null,

        val name: String? = null,

        @SerialName("original_name")
        val originalName: String? = null,

        val popularity: Double? = null,

        @SerialName("profile_path")
        val profilePath: String? = null,

        val character: String? = null,
        val order: Long? = null
    )


    @Serializable
    data class Company(
        val id: Long,
        @SerialName("logo_path")
        val logoPath: String? = null,
        val name: String,
        @SerialName("origin_country")
        val originCountry: String? = null
    )
}