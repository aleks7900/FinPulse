package com.finpulse.app.domain.model.sync

import kotlinx.serialization.Serializable

@Serializable
data class CloudUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)
