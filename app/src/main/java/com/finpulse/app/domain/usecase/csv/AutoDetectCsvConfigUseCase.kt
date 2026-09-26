package com.finpulse.app.domain.usecase.csv

import com.finpulse.app.domain.engine.CsvDetectorEngine
import com.finpulse.app.domain.model.CsvColumnMapping
import com.finpulse.app.domain.model.CsvFormatConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoDetectCsvConfigUseCase {

    suspend operator fun invoke(csvText: String): Pair<CsvFormatConfig, CsvColumnMapping> =
        withContext(Dispatchers.Default) {
            CsvDetectorEngine.detectConfigAndMapping(csvText)
        }
}
