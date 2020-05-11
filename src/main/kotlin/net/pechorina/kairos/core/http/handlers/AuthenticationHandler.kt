package net.pechorina.kairos.core.http.handlers

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.auth.AuthType
import net.pechorina.kairos.core.auth.UserDetails
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.ContentType
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.utils.JsonMapper
import java.nio.charset.StandardCharsets
import java.util.Base64

class AuthenticationHandler(val usernamePassAuthAddress: String?, val jwtAuthAddress: String? = null, val vertx: Vertx) : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        val authentication = when (detectAuthType(rc)) {
            AuthType.Basic -> doBasicAuth(rc)
            AuthType.JWT -> doJWTAuth(rc)
            else -> Future.succeededFuture(AuthenticationResult(noAuth = true))
        }

        authentication
                .onSuccess { result ->
                    val authenticationResult = result ?: AuthenticationResult(noAuth = true)
                    when {
                        authenticationResult.noAuth -> log.debug { "no authentication" }
                        authenticationResult.success() -> {
                            log.debug { "Authentication successful: ${authenticationResult.userDetails}" }
                            rc.put("authentication", authenticationResult.userDetails)
                        }
                        else -> log.debug { "Authentication failed: ${authenticationResult.failure}" }

                    }
                    rc.next()
                }
                .onFailure {
                    log.warn("Authentication error", it)
                    rc.next()
                }
    }


    private fun doBasicAuth(rc: RoutingContext): Future<AuthenticationResult> {
        if (usernamePassAuthAddress.isNullOrEmpty()) return Future.succeededFuture(AuthenticationResult(noAuth = true))

        log.debug("Perform basic authentication")
        val authHeader = rc.request().getHeader("Authorization")
        val credentials = getBasicCredentials(authHeader)

        val credentialsEvent = KEvent(
                JsonObject()
                        .put("username", credentials.first)
                        .put("password", credentials.second)
        )

        val promise = Promise.promise<AuthenticationResult>()

        vertx.eventBus().request<KEvent>(usernamePassAuthAddress, credentialsEvent) { replyResult ->
            if (replyResult.succeeded()) {
                val message = replyResult.result()
                val event = message.body()

                log.debug("UsernamePassword AuthService reply: {}", event)
                if (event.getStatus() == Status.OK) {
                    val userDetails = event.getPayloadAsJsonObject().mapTo(UserDetails::class.java)
                    promise.complete(AuthenticationResult(userDetails = userDetails))
                } else {
                    promise.complete(AuthenticationResult(failure = event.getPayloadAsString()))
                }
            } else {
                promise.complete(AuthenticationResult(failure = replyResult.cause().message))
            }
        }

        return promise.future()
    }

    private fun doJWTAuth(rc: RoutingContext): Future<AuthenticationResult> {
        if (jwtAuthAddress.isNullOrEmpty()) return Future.succeededFuture(AuthenticationResult(noAuth = true))

        log.debug("Perform JWT authentication")
        val token = getJwtToken(rc)
        val promise = Promise.promise<AuthenticationResult>()
        val tokenEvent = KEvent(token)

        vertx.eventBus().request<KEvent>(jwtAuthAddress, tokenEvent) { replyResult ->
            if (replyResult.succeeded()) {
                val message = replyResult.result()
                val event = message.body()

                log.debug("JWT AuthService reply: {}", event)
                if (event.getStatus() == Status.OK) {
                    val userDetails = JsonMapper.jsonObjectMapper.readValue(event.getPayloadAsString(), UserDetails::class.java)
                    promise.complete(AuthenticationResult(userDetails = userDetails))
                } else {
                    promise.complete(AuthenticationResult(failure = event.getPayloadAsString()))
                }
            } else {
                promise.complete(AuthenticationResult(failure = replyResult.cause().message))
            }
        }

        return promise.future()
    }

    private fun detectAuthType(rc: RoutingContext): AuthType {
        val authHeader = rc.request().getHeader("Authorization")
        log.debug("auth header: {}", authHeader)
        if (authHeader != null) {
            if (authHeader.toLowerCase().startsWith("basic")) {
                return AuthType.Basic
            } else if (authHeader.toLowerCase().startsWith("bearer")) {
                return AuthType.JWT
            }
        }
        return AuthType.None
    }

    private fun getJwtToken(rc: RoutingContext): String {
        val authHeader = rc.request().getHeader("Authorization")
        return authHeader.substring("Bearer".length).trim { it <= ' ' }
    }

    private fun getBasicCredentials(authorization: String): Pair<String, String?> {
        // Authorization: Basic base64credentials
        val base64Credentials = authorization.substring("Basic".length).trim { it <= ' ' }
        val credentials = String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8)
        // credentials = username:password
        val parts: List<String> = credentials.split(":".toRegex(), 2).filter { it -> it.isNotBlank() }
        val username = parts.elementAt(0)
        val password: String? = parts.elementAtOrElse(1, { null })
        return Pair(first = username, second = password)
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }

    data class AuthenticationResult(val userDetails: UserDetails? = null, val failure: String? = null, val noAuth: Boolean = false) {
        fun success(): Boolean {
            return userDetails != null
        }
    }
}
