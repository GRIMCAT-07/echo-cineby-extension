package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.helpers.WebViewClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.providers.WebViewClientProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toAlbum
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toData
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toShelf
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toTrack
import dev.brahmkshatriya.echo.extension.cineby.DecryptWebViewRequest
import dev.brahmkshatriya.echo.extension.cineby.ServerWebViewRequest
import dev.brahmkshatriya.echo.extension.cineby.models.Home
import dev.brahmkshatriya.echo.extension.cineby.models.Media
import dev.brahmkshatriya.echo.extension.cineby.models.Media.Season
import dev.brahmkshatriya.echo.extension.cineby.models.Search
import dev.brahmkshatriya.echo.extension.cineby.models.Server
import dev.brahmkshatriya.echo.extension.cineby.models.Subtitle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.WeakHashMap

class CineByExtension : ExtensionClient, HomeFeedClient, AlbumClient, TrackClient,
    WebViewClientProvider, SearchFeedClient {

    override val settingItems: List<Setting> = emptyList()
    private lateinit var setting: Settings
    override fun setSettings(settings: Settings) {
        setting = settings
    }

    private lateinit var webViewClient: WebViewClient
    override fun setWebViewClient(webViewClient: WebViewClient) {
        this.webViewClient = webViewClient
    }

    companion object {
        const val BASE_URL = "https://www.cineby.app"
        const val LANG = "en"
        const val KEY = "ad301b7cc82ffe19273e55e4d4206885"
        val buildIdRegex = Regex("\"buildId\":\"(.{21})\"")
    }

    private val client by lazy { OkHttpClient.Builder().build() }
    private suspend fun call(url: String) = client.newCall(
        Request.Builder().url(url).build()
    ).await().body.string()

    private val idMutex = Mutex()
    private var id: String? = null
    private suspend fun getBaseId() = idMutex.withLock {
        id = if (id == null) call(BASE_URL).let { body ->
            buildIdRegex.find(body)?.groupValues?.getOrNull(1)
                ?: throw IllegalStateException("Failed to find build ID in response")
        } else id
        id!!
    }

    override suspend fun getHomeTabs() = listOf<Tab>()
    override fun getHomeFeed(tab: Tab?) = PagedData.Single<Shelf> {
        val id = getBaseId()
        val url = "$BASE_URL/_next/data/$id/$LANG.json"
        val response = call(url).toData<Home>()
        val page = response.pageProps
        val home = page.messages["Home"]?.jsonObject
        listOf(
            page.trendingSections.map { it.toShelf(home) },
            page.streamingSections.map { it.toShelf(home) },
            page.defaultSections.map { it.toShelf(home) },
            page.genreSections.map { it.toShelf(home) },
        ).flatten()
    }

    private val shelvesMap = WeakHashMap<String, List<Shelf>>()

    override suspend fun loadAlbum(album: Album): Album {
        val type = album.extras["type"] ?: "movie"
        val id = album.id
        val season = album.extras["season"]?.toInt()
        if (season != null) {
            val cover = album.cover as ImageHolder.UrlRequestImageHolder
            return album.copy(cover = cover.copy(crop = false))
        }
        val url =
            "https://db.cineby.app/3/$type/$id?append_to_response=similar,recommendations&language=$LANG&api_key=$KEY"
        val response = call(url).toData<Media>()
        if (response.similar != null || response.recommendations != null) {
            shelvesMap[id] = listOfNotNull(response.similar?.let { similar ->
                Shelf.Lists.Items(
                    "Similar", similar.results.map { it.toAlbum(true).toMediaItem() })
            }, response.recommendations?.let { recs ->
                Shelf.Lists.Items(
                    "Recommendations", recs.results.map { it.toAlbum(true).toMediaItem() })
            })
        }
        return response.toAlbum()
    }

    override fun getShelves(album: Album) = PagedData.Single {
        val seasons = album.extras["seasons"]?.toData<List<Season>?>()?.let { list ->
            Shelf.Lists.Items(
                "Seasons", list.map { it.toAlbum(album).toMediaItem() })
        }
        val id = album.id
        val shelves = shelvesMap[id].orEmpty()
        listOfNotNull(seasons) + shelves
    }

    override fun loadTracks(album: Album) = PagedData.Single {
        val type = album.extras["type"] ?: "movie"
        if (type == "movie") listOf(album.toTrack()) else {
            val season = album.extras["season"]?.toIntOrNull() ?: 1
            val url =
                "https://db.cineby.app/3/tv/${album.id}/season/$season?language=$LANG&api_key=$KEY"
            val response = call(url).toData<Season>()
            response.episodes!!.map { it.toTrack(album) }
        }
    }

    suspend fun getSubs(track: Track): List<Streamable> {
        val type = track.album!!.extras["type"] ?: "movie"
        val id = track.album!!.id
        val url = if (type != "movie") {
            val season = track.album!!.extras["season"]?.toIntOrNull()!!
            val episode = track.extras["episode"]?.toIntOrNull()!!
            "https://sub.wyzie.ru/search?id=$id&season=$season&episode=$episode&format=srt"
        } else {
            "https://sub.wyzie.ru/search?id=$id&format=srt"
        }
        return call(url).toData<List<Subtitle>>().take(15).map {
            Streamable.subtitle(it.url!!, it.display)
        }
    }

    override suspend fun loadTrack(track: Track) = coroutineScope {
        if (track.streamables.isNotEmpty()) return@coroutineScope track
        val subs = async { runCatching { getSubs(track) }.getOrNull().orEmpty() }
        val streamables = webViewClient.await(
            false, "Getting Servers", ServerWebViewRequest(track.id.toRequest(), track.album!!.id)
        ).getOrThrow()!!.toData<List<Streamable>>().sortedBy { it.title != "Neon" }
        track.copy(streamables = streamables + subs.await())
    }

    override fun getShelves(track: Track): PagedData<Shelf> {
        TODO("Not yet implemented")
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean
    ): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                val data = call(streamable.id)
                val id = streamable.extras["id"]!!
                val url = streamable.extras["url"]!!.toRequest()
                val decrypted = webViewClient.await(
                    false, "Loading ${streamable.title}", DecryptWebViewRequest(url, data, id)
                ).getOrThrow()!!.toData<Server>().sources.mapNotNull {
                    val quality = it.quality?.removeSuffix("p")?.toIntOrNull()
                    val link = it.url.jsonPrimitive.contentOrNull ?: return@mapNotNull null
                    Streamable.Source.Http(
                        title = it.quality,
                        request = link.toRequest(mapOf("referer" to "https://www.cineby.app")),
                        quality = quality ?: 0,
                        type = if (link.endsWith(".m3u8")) Streamable.SourceType.HLS
                        else Streamable.SourceType.Progressive
                    )
                }
                Streamable.Media.Server(decrypted, false)
            }

            Streamable.MediaType.Subtitle -> Streamable.Media.Subtitle(
                streamable.id, Streamable.SubtitleType.SRT
            )

            else -> throw IllegalStateException()
        }
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        val history = getHistory().toMutableList()
        history.remove(item.title)
        setting.putString("search_history", history.joinToString(","))
    }

    private fun getHistory() = setting.getString("search_history")
        ?.split(",")?.distinct()?.take(5)
        ?: emptyList()

    private fun saveInHistory(query: String) {
        val history = getHistory().toMutableList()
        history.add(0, query)
        setting.putString("search_history", history.joinToString(","))
    }

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return getHistory().map { QuickSearchItem.Query(it, true) }
    }

    override fun searchFeed(query: String, tab: Tab?): PagedData<Shelf> {
        return if (query.isBlank()) PagedData.Single {
            val id = getBaseId()
            val resp = call("$BASE_URL/_next/data/$id/$LANG/search.json").toData<Search>()
            resp.pageProps.trending.map { it.toAlbum().toMediaItem().toShelf() }
        } else PagedData.Single {
            saveInHistory(query)
            val type = tab?.id ?: "multi"
            val url =
                "https://db.cineby.app/3/search/$type?page=1&query=$query&language=$LANG&api_key=$KEY"
            val res = call(url)
            res.toData<Media.Search>().results.map { it.toAlbum(true).toMediaItem().toShelf() }
        }
    }

    override suspend fun searchTabs(query: String) = if (query.isBlank()) listOf() else listOf(
        Tab("multi", "All"), Tab("movie", "Movies"), Tab("tv", "TV Shows")
    )
}