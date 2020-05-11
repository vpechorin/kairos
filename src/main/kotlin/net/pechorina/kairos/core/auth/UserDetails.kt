package net.pechorina.kairos.core.auth

data class UserDetails(val username: String, val authorities: Set<String> = emptySet(), val anonymous: Boolean = false)
