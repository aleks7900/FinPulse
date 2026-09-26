package com.finpulse.app

import android.app.Application
import com.finpulse.app.di.AppContainer
import com.finpulse.app.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Seed default categories & rules in background
        applicationScope.launch {
            container.categoryRepository.seedDefaultCategoriesIfNeeded()
            container.categorizationRuleRepository.seedDefaultRulesIfNeeded()
        }

        // Schedule periodic WorkManager recurring check & reminders
        com.finpulse.app.core.work.RecurringCheckWorker.schedulePeriodicCheck(this)
    }
}
