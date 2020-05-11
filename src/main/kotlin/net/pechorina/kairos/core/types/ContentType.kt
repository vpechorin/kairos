package net.pechorina.kairos.core.types

enum class ContentType(val mediaType: String) {
    BINARY("application/octet-stream"),
    JSON("application/json"),
    JSONA("application/json+array"),
    XML("application/xml"),
    XHTML("application/xhtml"),
    HTML("text/html"),
    TEXT("text/plain"),
    INT("text/int"),
    LONG("text/long"),
    BOOL("text/boolean");
}

fun mapStringToContentType(mediaType: String?, default: ContentType): ContentType {
    if (mediaType == null) return default
    val s = ContentType.values().firstOrNull { it.mediaType.equals(mediaType, true) }
    return s ?: default
}

fun mapNameToContentType(name: String?, default: ContentType): ContentType {
    if (name == null) return default
    val s = ContentType.values().firstOrNull { it.name.equals(name, true) }
    return s ?: default
}