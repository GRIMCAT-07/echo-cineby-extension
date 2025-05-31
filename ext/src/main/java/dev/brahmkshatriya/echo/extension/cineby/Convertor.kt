package dev.brahmkshatriya.echo.extension.cineby

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Date
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.cineby.models.Media
import dev.brahmkshatriya.echo.extension.cineby.models.Section
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object Convertor {
    val json by lazy { Json { ignoreUnknownKeys = true } }
    inline fun <reified T> String.toData() =
        runCatching { json.decodeFromString<T>(this) }.getOrElse {
            throw IllegalStateException("Failed to parse JSON: $this", it)
        }

    inline fun <reified T> T.toJson() = json.encodeToString(this)

    fun Section.toShelf(message: JsonObject?): Shelf.Lists.Items {
        val custom =
            if ("toprated" !in name) null else name.replace("toprated(.*)".toRegex()) { result ->
                val type = result.groupValues.getOrNull(1)?.lowercase() ?: return@replace ""
                "Top Rated ${type.replaceFirstChar { it.uppercase() }}"
            }.takeIf { it.isNotBlank() }

        return Shelf.Lists.Items(
            title = custom ?: message?.get(name)?.jsonPrimitive?.content ?: name.camelToPascal(),
            list = movies.map { it.toAlbum().toMediaItem() }
        )
    }

    private fun createSubtitle(type: String, date: Date?, rating: Double?) = buildString {
        append(if (type == "tv") "\uD83D\uDCFA" else "\uD83C\uDFAC")
        date?.let { append(" ${it.year}") }
        rating?.takeIf { it > 0 }?.let { append("\n${it.toString().take(3)}★") }
    }

    fun Section.Movie.toAlbum(): Album {
        val date = releaseDate.toDate()
        return Album(
            id = id.toString(),
            title = title,
            description = description,
            cover = poster.toImageHolder(crop = true),
            subtitle = createSubtitle(mediaType, date, rating),
            releaseDate = date,
            tracks = if (mediaType == "tv") null else 1,
            extras = mapOf("type" to mediaType)
        )
    }

    private fun String.toDate(): Date? {
        val parts = split("-")
        return Date(
            year = parts.getOrNull(0)?.toIntOrNull() ?: return null,
            month = parts.getOrNull(1)?.toIntOrNull(),
            day = parts.getOrNull(2)?.toIntOrNull()
        )
    }

    private fun String.camelToPascal() = replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        .trim()

    private const val TMDB_IMAGE = "https://image.tmdb.org/t/p/w500"

    fun Media.toAlbum(crop: Boolean = false): Album {
        val date = (releaseDate ?: firstAirDate ?: lastAirDate)?.toDate()
        val type = (type ?: mediaType ?: "movie")
        val title = name ?: title ?: originalName ?: originalTitle ?: "Unknown"
        return Album(
            id = id.toString(),
            title = title,
            description = if (tagline.isNullOrBlank()) overview else "$tagline\n\n$overview",
            cover = if (posterPath != null) "$TMDB_IMAGE$posterPath".toImageHolder(crop = crop) else null,
            releaseDate = date,
            subtitle = createSubtitle(type, date, voteAverage),
            tracks = seasons?.firstOrNull()?.episodeCount ?: (if (type == "tv") null else 1),
            duration = runtime?.let { it * 60 * 1000 },
            artists = (networks ?: productionCompanies).orEmpty().map { it.toArtist() },
            isExplicit = adult,
            extras = mapOf(
                "type" to type,
                "title" to title,
                "seasons" to seasons.toJson(),
            )
        )
    }

    fun Media.Season.toAlbum(album: Album): Album {
        val date = airDate?.toDate()
        val title = album.extras["title"]!!
        return album.copy(
            id = album.id,
            title = title + " " + (name ?: "Season $seasonNumber"),
            description = overview,
            cover = "$TMDB_IMAGE$posterPath".toImageHolder(crop = true),
            subtitle = createSubtitle("tv", date, voteAverage),
            releaseDate = date,
            tracks = episodeCount,
            extras = album.extras + mapOf("season" to seasonNumber.toString())
        )
    }

    private fun Media.Company.toArtist() = Artist(
        id = id.toString(),
        name = name,
    )

    fun Album.toTrack() = Track(
        id = "https://www.cineby.app/movie/${id}?play=true",
        title = title,
        artists = artists,
        cover = cover,
        album = this,
        duration = duration,
        isExplicit = isExplicit,
        releaseDate = releaseDate
    )

    fun Media.Episode.toTrack(album: Album): Track {
        val date = airDate?.toDate()
        val season = album.extras["season"]?.toIntOrNull() ?: 1
        return Track(
            id = "https://www.cineby.app/tv/${album.id}/$season/$episodeNumber?play=true",
            title = name ?: "Episode $episodeNumber",
            artists = album.artists,
            cover = "$TMDB_IMAGE$stillPath".toImageHolder(crop = true),
            duration = runtime?.let { it * 60 * 1000 },
            isExplicit = album.isExplicit,
            releaseDate = date,
            album = album,
            extras = mapOf("episode" to episodeNumber.toString())
        )
    }
}