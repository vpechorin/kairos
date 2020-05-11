package net.pechorina.kairos.core.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import org.slf4j.LoggerFactory

class KUser(val userDetails: UserDetails) : User {

    @JsonIgnore
    private var authProvider: AuthProvider? = null

    override fun isAuthorized(authority: String, resultHandler: Handler<AsyncResult<Boolean>>): User {
        if (authority.isNullOrEmpty()) {
            resultHandler.handle(Future.failedFuture("emptyPermission"))
            return this
        }
        val result = userDetails.authorities.contains(authority)
        resultHandler.handle(Future.succeededFuture(result))
        return this
    }

    override fun clearCache(): User {
        return this
    }

    override fun principal(): JsonObject {
        return JsonObject.mapFrom(userDetails)
    }

    override fun setAuthProvider(authProvider: AuthProvider) {
        this.authProvider = authProvider
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KUser

        if (userDetails != other.userDetails) return false
        if (authProvider != other.authProvider) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userDetails.hashCode()
        result = 31 * result + (authProvider?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "KUser(userDetails=$userDetails, authProvider=$authProvider)"
    }

    companion object {
        private val log = LoggerFactory.getLogger(KUser::class.java)
    }
}
