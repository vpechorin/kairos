package net.pechorina.kairos.core.verticles.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.auth.UsernamePasswordInMemoryAuthProvider
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.StageVerticle

class UsernamePasswordAuthService : StageVerticle() {
    private lateinit var usernamePasswordAuthProvider: AuthProvider

    override fun start() {
        super.start()
        this.usernamePasswordAuthProvider = UsernamePasswordInMemoryAuthProvider(
                "user.authorities",
                "credentials",
                vertx
        )
        subscribe(IOLaneType.EVENT, Handler { this.handler(it) })
    }

    private fun handler(message: Message<KEvent>) {
        log.debug("Incoming auth request: {}", message.body())
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
        val event = if (authResult.succeeded()) {
            log.warn("Authentication successful: ${authResult.result()}")
            KEvent(authResult.result().principal())
                    .extendPath(sourceEvent, id())
                    .setStatus(Status.OK)
        } else {
            log.warn("Authentication failed: ${authResult.cause().message}")
            KEvent(authResult.cause()?.message ?: "Authentication failed")
                    .extendPath(sourceEvent, id())
                    .setStatus(Status.UNAUTHORIZED)
        }

        inMessage.reply(event)
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
