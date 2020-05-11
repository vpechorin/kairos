package net.pechorina.kairos.core.http.handlers

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.auth.UserDetails
import java.util.concurrent.atomic.AtomicBoolean

class AuthorizationHandler(val authorities: Set<String> = emptySet()) : Handler<RoutingContext> {

    private val lock = Any()

    override fun handle(rc: RoutingContext) {
        val user: UserDetails?
        try {
            user = rc.get<UserDetails>("authentication")
        } catch (e: Exception) {
            log.debug("Unable to get authentication:", e)
            rc.next()
            return
        }

        if (user == null && !authorities.isEmpty()) {
            sendForbidden(rc)
            return
        }

        for (a in authorities) {
            if (user.authorities.contains(a)) {
                log.debug { "Success! User ${user.username} authorized for the required authority: $a" }
                rc.next()
                return
            }
            log.debug { "User ${user.username} is not authorized for the required authority: $a" }
        }
        log.debug { "User: [${user}] has no required authorities: [${authorities}]" }
        sendForbidden(rc)
    }

    private fun sendForbidden(routingContext: RoutingContext) {
        routingContext.response().statusCode = 403
        routingContext.response().statusMessage = "Forbidden"
        routingContext.response().end()
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }

}
