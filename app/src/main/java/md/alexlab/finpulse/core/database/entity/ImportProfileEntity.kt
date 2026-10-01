package md.alexlab.finpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import md.alexlab.finpulse.domain.model.AmountMode
import md.alexlab.finpulse.domain.model.CsvColumnMapping
import md.alexlab.finpulse.domain.model.CsvFormatConfig
import md.alexlab.finpulse.domain.model.ImportProfile

@Entity(tableName = "import_profiles")
data class ImportProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val institution: String? = null,
    val delimiter: String = ",",
    val dateFormat: String = "yyyy-MM-dd",
    val decimalSeparator: String = ".",
    val hasHeader: Boolean = true,
    val reversedDebitCredit: Boolean = false,
    val encoding: String = "UTF-8",
    val amountMode: String = "SINGLE_AMOUNT",
    val dateColumnIndex: Int = -1,
    val amountColumnIndex: Int = -1,
    val debitColumnIndex: Int = -1,
    val creditColumnIndex: Int = -1,
    val descriptionColumnIndex: Int = -1,
    val merchantColumnIndex: Int = -1,
    val currencyColumnIndex: Int = -1,
    val balanceColumnIndex: Int = -1,
    val categoryColumnIndex: Int = -1,
    val defaultAccountId: String? = null,
    val isSystemPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ImportProfile = ImportProfile(
        id = id,
        name = name,
        institution = institution,
        formatConfig = CsvFormatConfig(
            delimiter = delimiter.firstOrNull() ?: ',',
            dateFormat = dateFormat,
            decimalSeparator = decimalSeparator.firstOrNull() ?: '.',
            hasHeader = hasHeader,
            reversedDebitCredit = reversedDebitCredit,
            encoding = encoding,
            amountMode = try { AmountMode.valueOf(amountMode) } catch (_: Exception) { AmountMode.SINGLE_AMOUNT }
        ),
        columnMapping = CsvColumnMapping(
            dateColumnIndex = dateColumnIndex,
            amountColumnIndex = amountColumnIndex,
            debitColumnIndex = debitColumnIndex,
            creditColumnIndex = creditColumnIndex,
            descriptionColumnIndex = descriptionColumnIndex,
            merchantColumnIndex = merchantColumnIndex,
            currencyColumnIndex = currencyColumnIndex,
            balanceColumnIndex = balanceColumnIndex,
            categoryColumnIndex = categoryColumnIndex
        ),
        defaultAccountId = defaultAccountId,
        isSystemPreset = isSystemPreset,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(profile: ImportProfile): ImportProfileEntity = ImportProfileEntity(
            id = profile.id,
            name = profile.name,
            institution = profile.institution,
            delimiter = profile.formatConfig.delimiter.toString(),
            dateFormat = profile.formatConfig.dateFormat,
            decimalSeparator = profile.formatConfig.decimalSeparator.toString(),
            hasHeader = profile.formatConfig.hasHeader,
            reversedDebitCredit = profile.formatConfig.reversedDebitCredit,
            encoding = profile.formatConfig.encoding,
            amountMode = profile.formatConfig.amountMode.name,
            dateColumnIndex = profile.columnMapping.dateColumnIndex,
            amountColumnIndex = profile.columnMapping.amountColumnIndex,
            debitColumnIndex = profile.columnMapping.debitColumnIndex,
            creditColumnIndex = profile.columnMapping.creditColumnIndex,
            descriptionColumnIndex = profile.columnMapping.descriptionColumnIndex,
            merchantColumnIndex = profile.columnMapping.merchantColumnIndex,
            currencyColumnIndex = profile.columnMapping.currencyColumnIndex,
            balanceColumnIndex = profile.columnMapping.balanceColumnIndex,
            categoryColumnIndex = profile.columnMapping.categoryColumnIndex,
            defaultAccountId = profile.defaultAccountId,
            isSystemPreset = profile.isSystemPreset,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )
    }
}
