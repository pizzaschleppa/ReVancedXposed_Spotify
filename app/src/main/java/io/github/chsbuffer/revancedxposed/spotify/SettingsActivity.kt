package io.github.chsbuffer.revancedxposed.spotify

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ADBLOCK
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_MONET
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_PREMIUM
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ROUND_UI
import io.github.chsbuffer.revancedxposed.PREF_FILE
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.io.File

class SettingsActivity : ComponentActivity(), XposedServiceHelper.OnServiceListener {
    private var xposedService: XposedService? = null

    private val prefs by lazy {
        getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { XposedServiceHelper.registerListener(this) }

        makePrefsReadable()

        setContent {
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(this)
            } else {
                darkColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "ReVanced Xposed FE Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Changes apply after Spotify is restarted.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SettingsRow(
                        title = "Enable Premium",
                        subtitle = "Listen in any order, shuffle, or Smart Shuffle",
                        prefKey = PREF_ENABLE_PREMIUM,
                        defaultValue = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        title = "Enable AdBlock",
                        subtitle = "Block ads and other unwanted content",
                        prefKey = PREF_ENABLE_ADBLOCK,
                        defaultValue = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        title = "Enable Monet Theme by TheWinner02",
                        subtitle = "Dynamic colors based on the wallpaper",
                        prefKey = PREF_ENABLE_MONET,
                        defaultValue = false
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsRow(
                        title = "Enable RoundyUI by TheWinner02",
                        subtitle = "Rounded corners on cards and images",
                        prefKey = PREF_ENABLE_ROUND_UI,
                        defaultValue = false
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsRow(title: String, subtitle: String, prefKey: String, defaultValue: Boolean) {
        var checked by remember { mutableStateOf(prefs.getBoolean(prefKey, defaultValue)) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    checked = !checked
                    savePref(prefKey, checked)
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { isChecked ->
                    checked = isChecked
                    savePref(prefKey, isChecked)
                }
            )
        }
    }

    private fun savePref(key: String, value: Boolean) {
        prefs.edit(commit = true) { putBoolean(key, value) }
        runCatching {
            xposedService?.getRemotePreferences(PREF_FILE)?.edit()?.putBoolean(key, value)?.apply()
        }
        makePrefsReadable()
    }

    private fun makePrefsReadable() {
        runCatching {
            val prefsFile = File(applicationInfo.dataDir, "shared_prefs/$PREF_FILE.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        }
    }
}
