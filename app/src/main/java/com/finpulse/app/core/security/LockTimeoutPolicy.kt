package com.finpulse.app.core.security

import com.finpulse.app.R

enum class LockTimeoutPolicy(val timeoutMillis: Long, val displayNameResId: Int) {
    IMMEDIATELY(0L, R.string.security_timeout_immediately),
    SECONDS_30(30_000L, R.string.security_timeout_30s),
    MINUTE_1(60_000L, R.string.security_timeout_1m),
    MINUTES_5(300_000L, R.string.security_timeout_5m),
    MINUTES_15(900_000L, R.string.security_timeout_15m),
    NEVER(Long.MAX_VALUE, R.string.security_timeout_never);

    companion object {
        fun fromName(name: String?): LockTimeoutPolicy {
            return entries.find { it.name == name } ?: MINUTE_1
        }
    }
}
