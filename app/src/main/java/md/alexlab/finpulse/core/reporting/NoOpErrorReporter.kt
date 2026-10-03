package md.alexlab.finpulse.core.reporting

/**
 * No-op implementation of [ErrorReporter] for unit tests or environments where reporting is disabled.
 */
class NoOpErrorReporter : ErrorReporter {

    private var _enabled = false

    override val isCollectionEnabled: Boolean
        get() = _enabled

    override fun recordException(throwable: Throwable, context: Map<String, String>) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
    override fun setUserContext(userId: String?) = Unit
    override fun clearUserContext() = Unit
    override fun setCollectionEnabled(enabled: Boolean) {
        _enabled = enabled
    }
}
