package io.github.chsbuffer.revancedxposed.spotify

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import io.github.chsbuffer.revancedxposed.PREF_FILE
import io.github.chsbuffer.revancedxposed.spotify.ui.screen.HomeScreen
import io.github.chsbuffer.revancedxposed.spotify.ui.theme.AppTheme
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.io.File

class SettingsActivity : ComponentActivity(), XposedServiceHelper.OnServiceListener {
    private var xposedService: XposedService? = null

    private val isModuleActiveState = mutableStateOf(false)

    private val prefs by lazy {
        getSharedPreferences(PREF_FILE, MODE_PRIVATE)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        isModuleActiveState.value = true
        runCatching {
            val remotePrefs = service.getRemotePreferences(PREF_FILE)
            // Ensure remote preferences are seeded with any local settings
            val editor = remotePrefs.edit()
            var changed = false
            for ((k, v) in prefs.all) {
                if (v is Boolean && !remotePrefs.contains(k)) {
                    editor.putBoolean(k, v)
                    changed = true
                }
            }
            if (changed) editor.apply()
        }
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
        isModuleActiveState.value = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        runCatching { XposedServiceHelper.registerListener(this) }

        makePrefsReadable()

        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("unknown")

        setContent {
            AppTheme {
                HomeScreen(
                    prefs = prefs,
                    isModuleActive = isModuleActiveState.value,
                    versionName = versionName ?: "unknown",
                    onPrefChanged = ::savePref,
                )
            }
        }
    }

    private fun savePref(key: String, value: Boolean) {
        prefs.edit(commit = true) { putBoolean(key, value) }
        runCatching {
            xposedService?.getRemotePreferences(PREF_FILE)?.edit { putBoolean(key, value) }
        }
        makePrefsReadable()
    }

    @SuppressLint("SetWorldReadable")
    private fun makePrefsReadable() {
        runCatching {
            val prefsFile = File(applicationInfo.dataDir, "shared_prefs/$PREF_FILE.xml")
            if (prefsFile.exists()) {
                @Suppress("SetWorldReadable")
                prefsFile.setReadable(true, false)
            }
        }
    }
}
