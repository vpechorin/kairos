package net.pechorina.kairos.core.verticles.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.jwt.JWTOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.auth.jwt.jwtAuthOptionsOf
import io.vertx.kotlin.ext.auth.keyStoreOptionsOf
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.auth.UsernamePasswordInMemoryAuthProvider
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.http.handlers.AuthenticationHandler
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.StageVerticle

class JWTAuthService : StageVerticle() {
    lateinit var usernamePasswordAuthProvider: AuthProvider
    lateinit var jwtProvider: JWTAuth

    override fun start() {
        super.start()
        this.usernamePasswordAuthProvider = UsernamePasswordInMemoryAuthProvider(
                "user.authorities",
                "credentials",
                vertx
        )

        val vertxConfig = config().getJsonObject("config")

        val keyStoreOptions = keyStoreOptionsOf(
                path = "keystore.jceks",
                type = "jceks",
                password = vertxConfig.getString("keystorePassword")
        )

        val config = jwtAuthOptionsOf(keyStore = keyStoreOptions)

        this.jwtProvider = JWTAuth.create(vertx, config)

        subscribe(IOLaneType.INTERACTIVE, "new_", Handler { this.handleNewAuthentication(it) })
        subscribe(IOLaneType.INTERACTIVE, "check_", Handler { this.checkToken(it) })
    }

    private fun handleNewAuthentication(message: Message<KEvent>) {
        log.debug("Incoming new auth request: {}", message.body())

        val inEvent = message.body()
        val json = inEvent.getPayloadAsJsonObject()
        val user = json.getString("username")
        val password = json.getString("password")

        usernamePasswordAuthProvider.authenticate(
                JsonObject()
                        .put("username", user)
                        .put("password", password)

        ) { h -> authResponseHandler(message, inEvent, h) }
    }

    private fun authResponseHandler(inMessage: Message<KEvent>, sourceEvent: KEvent, authResult: AsyncResult<User>) {

        val outEvent = if (authResult.succeeded()) {
            log.warn("Authentication successful: {}", authResult.result())
            val token = jwtProvider.generateToken(getClaims(authResult.result()), getJWTOptions(authResult.result()))
            KEvent(authResult.result().principal())
                    .extendPath(sourceEvent, id())
                    .setStatus(Status.OK)
                    .addHeader("jwt", token)
        } else {
            log.warn("Authentication failed: {}", authResult.cause().message)
            KEvent(authResult.cause()?.message ?: "Authentication failed")
                    .extendPath(sourceEvent, id())
                    .setStatus(Status.UNAUTHORIZED)
        }

        inMessage.reply(outEvent)
    }

    private fun checkToken(message: Message<KEvent>) {
        log.debug("Incoming check token request: {}", message.body())

        val inEvent = message.body()
        val token = inEvent.getPayloadAsString()

        jwtProvider.authenticate(json {
            obj("jwt" to token)
        }) { result ->
            if (result.succeeded()) {
                var theUser = result.result()
                AuthenticationHandler.log.debug("JWT auth success: ${theUser}")
                message.reply(
                        KEvent(theUser.principal())
                                .extendPath(inEvent, id())
                                .setStatus(Status.OK)
                                .addHeader("jwt", token)
                )
            } else {
                KEvent(result.cause()?.message ?: "Authentication failed")
                        .extendPath(inEvent, id())
                        .setStatus(Status.UNAUTHORIZED)
            }
        }
    }

    private fun getClaims(user: User): JsonObject {
        return user.principal()
    }

    private fun getJWTOptions(user: User): JWTOptions {
        val options = JWTOptions()
        user.principal()
                .getJsonArray("authorities")
                .forEach { authority -> options.addPermission(authority?.toString()) }
        options.expiresInSeconds = 86400
        return options
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
