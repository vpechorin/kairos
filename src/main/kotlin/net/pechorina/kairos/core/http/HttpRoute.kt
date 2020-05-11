package net.pechorina.kairos.core.http

import net.pechorina.kairos.core.types.ContentType

class HttpRoute {
    var id: String? = null
    var route: String? = null
    var method: HttpMethod? = null
    var consumes: String? = null
    var produces: String? = null
    var output: String? = null
    var schema: String? = null
    var validate: Boolean = false
    var timeout: Long? = null
    var authorities: String? = null
    var requestMapperEx: String? = null
    var requestMapperOutputType: ContentType? = null
    var responseMapperEx: String? = null
    var proxyPass: String? = null
    var proxyPassEx: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HttpRoute

        if (id != other.id) return false
        if (route != other.route) return false
        if (method != other.method) return false
        if (consumes != other.consumes) return false
        if (produces != other.produces) return false
        if (output != other.output) return false
        if (schema != other.schema) return false
        if (validate != other.validate) return false
        if (timeout != other.timeout) return false
        if (authorities != other.authorities) return false
        if (requestMapperEx != other.requestMapperEx) return false
        if (requestMapperOutputType != other.requestMapperOutputType) return false
        if (responseMapperEx != other.responseMapperEx) return false
        if (proxyPass != other.proxyPass) return false
        if (proxyPassEx != other.proxyPassEx) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (route?.hashCode() ?: 0)
        result = 31 * result + (method?.hashCode() ?: 0)
        result = 31 * result + (consumes?.hashCode() ?: 0)
        result = 31 * result + (produces?.hashCode() ?: 0)
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + (schema?.hashCode() ?: 0)
        result = 31 * result + validate.hashCode()
        result = 31 * result + (timeout?.hashCode() ?: 0)
        result = 31 * result + (authorities?.hashCode() ?: 0)
        result = 31 * result + (requestMapperEx?.hashCode() ?: 0)
        result = 31 * result + (requestMapperOutputType?.hashCode() ?: 0)
        result = 31 * result + (responseMapperEx?.hashCode() ?: 0)
        result = 31 * result + (proxyPass?.hashCode() ?: 0)
        result = 31 * result + (proxyPassEx?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "HttpRoute(id=$id, route=$route, method=$method, consumes=$consumes, produces=$produces, output=$output, schema=$schema, validate=$validate, timeout=$timeout, authorities=$authorities, requestMapperEx=$requestMapperEx, requestMapperOutputType=$requestMapperOutputType, responseMapperEx=$responseMapperEx, proxyPass=$proxyPass, proxyPassEx=$proxyPassEx)"
    }

}
