@file:Suppress("ktlint:standard:function-naming")

package io.github.chsbuffer.revancedxposed.spotify.ui.screen

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ADBLOCK
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_MONET
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_PREMIUM
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ROUND_UI
import io.github.chsbuffer.revancedxposed.spotify.ui.component.ModuleStatusCard
import io.github.chsbuffer.revancedxposed.spotify.ui.component.SectionCard
import io.github.chsbuffer.revancedxposed.spotify.ui.component.SwitchPreference
import io.github.chsbuffer.revancedxposed.spotify.ui.theme.AppTheme
import io.github.chsbuffer.revancedxposed.spotify.ui.theme.Tokens

/*
 * @param prefs SharedPreferences instance (null in preview mode).
 * @param isModuleActive Whether the Xposed module is currently activated.
 * @param versionName The app / module version string.
 * @param onPrefChanged Callback to persist a preference change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: SharedPreferences?,
    isModuleActive: Boolean,
    versionName: String,
    onPrefChanged: (key: String, value: Boolean) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isPreview = LocalInspectionMode.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "ReVanced Xposed FE",
                        fontWeight = FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(Tokens.SpacingLg),
        ) {
            // ── Status card ──
            item {
                ModuleStatusCard(
                    isActive = isModuleActive,
                    versionName = versionName,
                    modifier = Modifier.padding(
                        horizontal = Tokens.ScreenHorizontalPadding,
                        vertical = Tokens.SpacingSm,
                    ),
                )
            }

            // ── Core features ──
            item {
                SectionCard(
                    title = "Core",
                    modifier = Modifier.padding(horizontal = Tokens.ScreenHorizontalPadding),
                ) {
                    val enablePremium = rememberPrefState(prefs, PREF_ENABLE_PREMIUM, true, isPreview)
                    SwitchPreference(
                        title = "Enable Premium",
                        subtitle = "Listen in any order, shuffle, or Smart Shuffle",
                        checked = enablePremium.value,
                        onCheckedChange = {
                            enablePremium.value = it
                            onPrefChanged(PREF_ENABLE_PREMIUM, it)
                        },
                        icon = Icons.Outlined.WorkspacePremium,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Tokens.PreferencePadding),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    val enableAdblock = rememberPrefState(prefs, PREF_ENABLE_ADBLOCK, true, isPreview)
                    SwitchPreference(
                        title = "Enable AdBlock",
                        subtitle = "Block ads and other unwanted content",
                        checked = enableAdblock.value,
                        onCheckedChange = {
                            enableAdblock.value = it
                            onPrefChanged(PREF_ENABLE_ADBLOCK, it)
                        },
                        icon = Icons.Outlined.Block,
                    )
                }
            }

            // ── Visual tweaks ──
            item {
                SectionCard(
                    title = "Appearance",
                    modifier = Modifier.padding(horizontal = Tokens.ScreenHorizontalPadding),
                ) {
                    val enableMonet = rememberPrefState(prefs, PREF_ENABLE_MONET, false, isPreview)
                    SwitchPreference(
                        title = "Monet Theme",
                        subtitle = "Dynamic colors based on your wallpaper",
                        checked = enableMonet.value,
                        onCheckedChange = {
                            enableMonet.value = it
                            onPrefChanged(PREF_ENABLE_MONET, it)
                        },
                        icon = Icons.Outlined.Palette,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Tokens.PreferencePadding),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    val enableRoundUI = rememberPrefState(prefs, PREF_ENABLE_ROUND_UI, false, isPreview)
                    SwitchPreference(
                        title = "Roundy UI",
                        subtitle = "Rounded corners on cards and images",
                        checked = enableRoundUI.value,
                        onCheckedChange = {
                            enableRoundUI.value = it
                            onPrefChanged(PREF_ENABLE_ROUND_UI, it)
                        },
                        icon = Icons.Outlined.RoundedCorner,
                    )
                }
            }

            // ── Footer note ──
            item {
                Text(
                    text = "Changes apply after Spotify is restarted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Tokens.ScreenHorizontalPadding + Tokens.PreferencePadding,
                        vertical = Tokens.SpacingSm,
                    ),
                )
                Spacer(modifier = Modifier.height(Tokens.SpacingLg))
            }
        }
    }
}

// ── Helpers ──

@Composable
private fun rememberPrefState(
    prefs: SharedPreferences?,
    key: String,
    defaultValue: Boolean,
    isPreview: Boolean,
) = remember {
    mutableStateOf(if (isPreview || prefs == null) defaultValue else prefs.getBoolean(key, defaultValue))
}

// ── Preview ──

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(
            prefs = null,
            isModuleActive = true,
            versionName = "1.0.0-preview",
            onPrefChanged = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenDarkPreview() {
    AppTheme {
        HomeScreen(
            prefs = null,
            isModuleActive = false,
            versionName = "1.0.0-preview",
            onPrefChanged = { _, _ -> },
        )
    }
}
