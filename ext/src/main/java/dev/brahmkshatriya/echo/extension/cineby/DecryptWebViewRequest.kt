package dev.brahmkshatriya.echo.extension.cineby

import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.extension.cineby.CineByJS.PAGE_START

class DecryptWebViewRequest(
    override val initialUrl: Request,
    data: String,
    id: String
) : WebViewRequest.Evaluate<String> {
    override val javascriptToEvaluate =
        "async function() { return await DECRYPT(\"$data\", \"\", $id) }"
    override val javascriptToEvaluateOnPageStart = PAGE_START
    override val stopUrlRegex = "https://backend\\.cineby\\.app/v1/.*".toRegex()

    override suspend fun onStop(url: Request, data: String?): String {
        return data!!
    }
}