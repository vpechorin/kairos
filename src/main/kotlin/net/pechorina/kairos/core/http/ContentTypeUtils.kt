package net.pechorina.kairos.core.http

import com.google.common.net.MediaType
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.RoutingContext
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.verticles.http.HttpServerSource

object ContentTypeUtils {
    val DEFAULT_CONTENT_TYPE = ContentType.TEXT
    val SIMPLE_JSON_MEDIATYPE = MediaType.create("application", "json")

    fun getAllContentTypes(contentTypes: String?): List<ContentType> {
        return when (contentTypes) {
            null -> emptyList()
            else -> contentTypes.split(",".toRegex())
                    .filter { !it.isNullOrBlank() }
                    .map { getContentType(it) }
        }
    }

    fun getContentType(value: String?): ContentType {
        if (value.isNullOrBlank()) return DEFAULT_CONTENT_TYPE
        try {
            return when (MediaType.parse(value)) {
                MediaType.JSON_UTF_8 -> ContentType.JSON
                SIMPLE_JSON_MEDIATYPE -> ContentType.JSON
                MediaType.XML_UTF_8 -> ContentType.XML
                MediaType.APPLICATION_XML_UTF_8 -> ContentType.XML
                MediaType.PLAIN_TEXT_UTF_8 -> ContentType.TEXT
                MediaType.XHTML_UTF_8 -> ContentType.XHTML
                MediaType.HTML_UTF_8 -> ContentType.HTML
                MediaType.ANY_TEXT_TYPE -> ContentType.TEXT
                else -> DEFAULT_CONTENT_TYPE
            }
        } catch (ex: IllegalArgumentException) {
            return DEFAULT_CONTENT_TYPE
        }
    }

    fun isJsonArray(value: String): Boolean {
        return value.trimStart().startsWith("[")
    }
}