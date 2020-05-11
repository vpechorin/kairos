package net.pechorina.kairos.core.http

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE;

    companion object {
        fun of(value: String): HttpMethod? {
            return HttpMethod.values()
                    .filter { m -> m.name.equals(value, ignoreCase = true) }
                    .first()
        }
    }
}
