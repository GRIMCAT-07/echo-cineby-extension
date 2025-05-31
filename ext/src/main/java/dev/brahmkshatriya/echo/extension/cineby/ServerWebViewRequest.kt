package dev.brahmkshatriya.echo.extension.cineby

import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extension.cineby.CineByJS.GET_SERVERS
import dev.brahmkshatriya.echo.extension.cineby.CineByJS.PAGE_START
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toData
import dev.brahmkshatriya.echo.extension.cineby.Convertor.toJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl

class ServerWebViewRequest(
    override val initialUrl: Request,
    private val id: String,
) : WebViewRequest.Evaluate<String> {

    override val javascriptToEvaluateOnPageStart = PAGE_START
    override val javascriptToEvaluate = GET_SERVERS
    override val stopUrlRegex = "https://backend\\.cineby\\.app/v1/.*".toRegex()

    override suspend fun onStop(url: Request, data: String?): String {
        return data!!.toData<List<Server>>().mapNotNull { server ->
            val req = runCatching { server.result.sources.jsonObject["e"]?.jsonPrimitive?.content }
                .getOrNull() ?: return@mapNotNull null
            val params = runCatching {
                server.result.sources.jsonObject["t"]?.jsonObject?.get("params")?.jsonObject
            }.getOrNull() ?: return@mapNotNull null
            val httpUrl = req.toHttpUrl().newBuilder()
            params.forEach { entry ->
                httpUrl.addQueryParameter(
                    entry.key,
                    entry.value.jsonPrimitive.let { it.contentOrNull ?: it.toString() })
            }
            val link = httpUrl.build().toString()
            Streamable.server(
                link, 0, server.name, extras = mapOf("id" to id, "url" to initialUrl.url)
            )
        }.toJson()
    }

    @Serializable
    data class Server(val name: String, val result: Result)

    @Serializable
    data class Result(val sources: JsonElement)
}