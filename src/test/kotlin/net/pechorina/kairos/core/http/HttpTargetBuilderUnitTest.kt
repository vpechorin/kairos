package net.pechorina.kairos.core.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpTargetBuilderUnitTest {
    @Test
    fun processFullPath() {
        val route = "/api/tags/:id"
        val pathParams = mapOf("id" to "123")
        val queryParams = mapOf("param1" to listOf("1", "12"), "param2" to listOf("2"))
        val path = "/api/tags/123"
        val target = "http://testhost/service/service1?secret=xxx&trace-id=9999"

        val u = HttpTargetBuilder(
                pathParams = pathParams,
                currentPath = path,
                queryParams = queryParams,
                routePath = route,
                target = target
        ).build()

        assertThat(u).isEqualTo("http://testhost/service/service1?secret=xxx&trace-id=9999&param1=1&param1=12&param2=2");
    }

    @Test
    fun pathPrefixStrip() {
        val route = "/api/"
        val path = "/api/tags/123"
        val target = "http://testhost/service/service1"

        val u = HttpTargetBuilder(
                currentPath = path,
                routePath = route,
                target = target
        ).build()

        assertThat(u).isEqualTo("http://testhost/service/service1/tags/123");
    }

    @Test
    fun pathPrefixStrip_NullRoute() {
        val path = "/api/tags/123"
        val target = "http://testhost:8080/service/service1"

        val u = HttpTargetBuilder(
                currentPath = path,
                target = target
        ).build()

        assertThat(u).isEqualTo("http://testhost:8080/service/service1/api/tags/123");
    }

    @Test
    fun pathPrefix_WithNoPathOnTarget_PathShouldBeTransferred() {
        val route = "/api/"
        val path = "/api/tags/123"
        val target = "http://testhost:8080"

        val u = HttpTargetBuilder(
                currentPath = path,
                routePath = route,
                target = target
        ).build()

        assertThat(u).isEqualTo("http://testhost:8080/api/tags/123");
    }
}