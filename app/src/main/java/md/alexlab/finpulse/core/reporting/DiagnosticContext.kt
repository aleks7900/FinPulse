package md.alexlab.finpulse.core.reporting

/**
 * Standard diagnostic context constants and builder helpers for FinPulse.
 * Ensures consistent technical identifiers across the application without leaking user data.
 */
object DiagnosticContext {

    const val KEY_FEATURE = "feature"
    const val KEY_OPERATION = "operation"
    const val KEY_SYNC_MODE = "sync_mode"
    const val KEY_DB_OPERATION = "db_operation"
    const val KEY_IMPORT_EXPORT_OP = "import_export_op"
    const val KEY_APP_VERSION = "app_version"
    const val KEY_BUILD_TYPE = "build_type"
    const val KEY_STATUS = "status"

    // Feature values
    const val FEATURE_CLOUD_SYNC = "cloud_sync"
    const val FEATURE_BACKUP_EXPORT = "backup_export"
    const val FEATURE_CSV_IMPORT = "csv_import"
    const val FEATURE_DATABASE = "database"
    const val FEATURE_AUTH = "auth"
    const val FEATURE_EXCHANGE_RATES = "exchange_rates"
    const val FEATURE_RECURRING = "recurring"
    const val FEATURE_DIGEST = "financial_digest"

    // Operation values
    const val OP_FULL_SYNC = "full_sync"
    const val OP_UPLOAD_CHANGES = "upload_changes"
    const val OP_DOWNLOAD_CHANGES = "download_changes"
    const val OP_CREATE_BACKUP = "create_backup"
    const val OP_RESTORE_BACKUP = "restore_backup"
    const val OP_EXPORT_CSV = "export_csv"
    const val OP_EXPORT_JSON = "export_json"
    const val OP_PARSE_CSV = "parse_csv"
    const val OP_EXECUTE_IMPORT = "execute_import"
    const val OP_SIGN_IN_GOOGLE = "sign_in_google"
    const val OP_SIGN_OUT = "sign_out"
    const val OP_FETCH_RATES = "fetch_rates"
    const val OP_RECURRING_CHECK = "recurring_check"

    /**
     * Builds a safe technical context map for an operation.
     */
    fun build(
        feature: String,
        operation: String,
        extra: Map<String, String> = emptyMap()
    ): Map<String, String> {
        val map = mutableMapOf(
            KEY_FEATURE to feature,
            KEY_OPERATION to operation
        )
        map.putAll(extra)
        return DataSanitizer.sanitizeContext(map)
    }
}
