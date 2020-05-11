package net.pechorina.kairos.core.http

import io.netty.handler.codec.http.QueryStringDecoder
import java.net.URI

class HttpTargetBuilder(val pathParams: Map<String, String> = emptyMap(),
                        val queryParams: Map<String, List<String>> = emptyMap(),
                        val currentPath: String = "",
                        val routePath: String? = null,
                        val target: String) {

    fun build(): String {
        val targetURI = URI(target)
        var targetPath = if (targetURI.path.isNullOrEmpty()) {
            currentPath
        } else {
            val currentPrefix = compileRouteTemplate(routePath, pathParams)
            if (currentPrefix.isNullOrEmpty().not() && currentPath.startsWith(currentPrefix!!, ignoreCase = true)) {
                currentPath.substringAfter(currentPrefix)
            } else {
                currentPath
            }
        }

        if (targetPath.isNullOrEmpty().not() && targetPath.startsWith("/").not()) targetPath = "/" + targetPath

        val uri = appendPath(targetURI, targetPath)
        return combineQueryParams(uri, queryParams).toString()
    }

    private fun compileRouteTemplate(path: String?, pathParams: Map<String, String>): String? {
        if (path.isNullOrEmpty()) return ""
        var result = path
        pathParams.forEach { k: String, v: String -> result = result!!.replace(":" + k, v) }
        return result
    }

    private fun appendPath(targetURI: URI, additionalPath: String): URI {
        var targetPath = targetURI.path ?: ""
        if (additionalPath.isNotBlank()) {
            targetPath += additionalPath
        }
        return URI(targetURI.scheme, null, targetURI.host, targetURI.port, targetPath, targetURI.query, targetURI.fragment)
    }

    private fun combineQueryParams(uri: URI, additionalParams: Map<String, List<String>>?): URI {
        if (additionalParams == null || additionalParams.isEmpty()) return uri;

        val decodedTargetQueryParams: Map<String, List<String>> = QueryStringDecoder(uri).parameters()
        val eventQueryPairs = mapListToPairs(decodedTargetQueryParams)
        val additionalPairs = mapListToPairs(additionalParams)
        val union = eventQueryPairs.union(additionalPairs).distinct()
        val queryString = union.joinToString("&") { "${it.first}=${it.second}" }

        return URI(uri.scheme, uri.userInfo, uri.host, uri.port, uri.path, queryString, uri.fragment)
    }

    private fun mapListToPairs(map: Map<String, List<String>>): List<Pair<String, String>> {
        return map.keys.flatMap { key ->
            map[key]
                    ?.map { Pair(key, it) }
                    .orEmpty()
        }
    }
}