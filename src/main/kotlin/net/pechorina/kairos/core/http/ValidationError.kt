package net.pechorina.kairos.core.http

data class ValidationError(
        var error: String? = null,
        var subject: String? = null,
        var message: String? = null,
        var exception: String? = null
)
