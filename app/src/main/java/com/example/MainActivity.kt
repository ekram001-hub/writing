package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository
import com.example.ui.navigation.MicroWriteNavApp
import com.example.ui.theme.MicroWriteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.repository.AppSettings()
            )

            MicroWriteTheme(themeMode = settings.themeMode) {
                MicroWriteNavApp()
            }
        }
    }
}
