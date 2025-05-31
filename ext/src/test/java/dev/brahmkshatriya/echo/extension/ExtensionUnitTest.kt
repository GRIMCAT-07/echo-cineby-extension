package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class ExtensionUnitTest {
    private val extension = CineByExtension()
    private val searchQuery = "Skrillex"

    // Test Setup
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        extension.setSettings(MockedSettings())
        runBlocking {
            extension.onExtensionSelected()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun testIn(title: String, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        println("\n-- $title --")
        block.invoke(this)
        println("\n")
    }

    // Actual Tests
    @Test
    fun testHomeFeed() = testIn("Testing Home Feed") {
        val feed = extension.getHomeFeed(null).loadList(null)
        val item =
            ((feed.data.first() as Shelf.Lists.Items).list[1] as EchoMediaItem.Lists.AlbumItem)
        val album = extension.loadAlbum(item.album)
        println("Album: $album")
        val shelves = extension.getShelves(album).loadList(null).data
        shelves.forEach {
            println(it)
        }
    }

    @Test
    fun testEmptyQuickSearch() = testIn("Testing Empty Quick Search") {
        val search = extension.quickSearch("")
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testQuickSearch() = testIn("Testing Quick Search") {
        val search = extension.quickSearch(searchQuery)
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testEmptySearch() = testIn("Testing Empty Search") {
        val tab = extension.searchTabs("").firstOrNull()
        val search = extension.searchFeed("", tab).loadList(null).data
        search.forEach {
            println(it)
        }
    }

    @Test
    fun testSearch() = testIn("Testing Search") {
        println("Tabs")
        extension.searchTabs(searchQuery).forEach {
            println(it.title)
        }
        println("Search Results")
        val search = extension.searchFeed(searchQuery, null).loadList(null).data
        search.forEach {
            println(it)
        }
    }

    private suspend fun searchTrack(q: String? = null): Track {
        val query = q ?: searchQuery
        println("Searching  : $query")
        val tab = extension.searchTabs(query).firstOrNull()
        val items = extension.searchFeed(query, tab).loadList(null).data
        val track = items.firstNotNullOfOrNull {
            when (it) {
                is Shelf.Item -> (it.media as? EchoMediaItem.TrackItem)?.track
                is Shelf.Lists.Tracks -> it.list.firstOrNull()
                is Shelf.Lists.Items -> (it.list.firstOrNull() as? EchoMediaItem.TrackItem)?.track
                else -> null
            }
        }
        return track ?: error("Track not found, try a different search query")
    }

    @Test
    fun testTrackGet() = testIn("Testing Track Get") {
        val search = searchTrack()
        measureTimeMillis {
            val track = extension.loadTrack(search)
            println(track)
        }.also { println("time : $it") }
    }

    @Test
    fun testTrackStream() = testIn("Testing Track Stream") {
        val feed = extension.getHomeFeed(null).loadList(null)
        val item =
            ((feed.data.first() as Shelf.Lists.Items).list[1] as EchoMediaItem.Lists.AlbumItem)
        val album = extension.loadAlbum(item.album)
        val track = extension.loadTracks(album).loadAll().first()
        println(extension.getSubs(track))
    }
}