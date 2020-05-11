package net.pechorina.kairos.core.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

class UsernamePasswordInMemoryAuthProvider(val userRolesMapName: String, val credentialsMapName: String, val vertx: Vertx) : AuthProvider {

    private val userRoles: Map<String, String>
        get() = vertx.sharedData().getLocalMap(userRolesMapName)

    private val credentials: Map<String, String>
        get() = vertx.sharedData().getLocalMap(credentialsMapName)

    override fun authenticate(authInfo: JsonObject, resultHandler: Handler<AsyncResult<User>>) {
        val username = authInfo.getString("username")
        if (username.isNullOrEmpty()) {
            resultHandler.handle(Future.failedFuture("Username is not specified"))
            return
        }

        val password = authInfo.getString("password")
        if (password.isNullOrEmpty()) {
            resultHandler.handle(Future.failedFuture("Password is not specified"))
            return
        }

        try {
            val secret = credentials[username]
            if (secret != null) {
                if (secret != password) {
                    resultHandler.handle(Future.failedFuture("Invalid password"))
                    return
                }
            } else {
                resultHandler.handle(Future.failedFuture("User not found"))
                return
            }

            val userDetails = UserDetails(username, getAuthorities(username))
            resultHandler.handle(Future.succeededFuture(KUser(userDetails)))
        } catch (e: RuntimeException) {
            resultHandler.handle(Future.failedFuture(e))
        }
    }

    fun getAuthorities(username: String): Set<String> {
        val authoritiesString = userRoles[username] ?: return emptySet()
        return authoritiesString
                .split(",".toRegex())
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .sorted()
                .toSet()
    }
}
