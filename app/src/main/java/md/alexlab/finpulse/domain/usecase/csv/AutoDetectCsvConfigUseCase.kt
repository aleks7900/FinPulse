package md.alexlab.finpulse.domain.usecase.csv

import md.alexlab.finpulse.domain.engine.CsvDetectorEngine
import md.alexlab.finpulse.domain.model.CsvColumnMapping
import md.alexlab.finpulse.domain.model.CsvFormatConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoDetectCsvConfigUseCase {

    suspend operator fun invoke(csvText: String): Pair<CsvFormatConfig, CsvColumnMapping> =
        withContext(Dispatchers.Default) {
            CsvDetectorEngine.detectConfigAndMapping(csvText)
        }
}
