package io.github.chsbuffer.revancedxposed.spotify

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ADBLOCK
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_MONET
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_PREMIUM
import io.github.chsbuffer.revancedxposed.PREF_ENABLE_ROUND_UI
import io.github.chsbuffer.revancedxposed.PREF_FILE
import java.io.File

class SettingsActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val colorSurface = themeColor(com.google.android.material.R.attr.colorSurface, "#121212".toColorInt())
        val colorSurfaceContainer = themeColor(com.google.android.material.R.attr.colorSurfaceContainer, "#1D1B20".toColorInt())
        val colorOnSurface = themeColor(com.google.android.material.R.attr.colorOnSurface, Color.WHITE)
        val colorOnSurfaceVariant = themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, "#CAC4D0".toColorInt())
    //    val colorPrimary = themeColor(com.google.android.material.R.attr.colorPrimary, "#1DB954".toColorInt())
        val colorOnPrimary = themeColor(com.google.android.material.R.attr.colorOnPrimary, Color.BLACK)

        window.statusBarColor = colorSurface
        window.navigationBarColor = colorSurface

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorSurface)
            setPadding(
                (24 * density).toInt(),
                (28 * density).toInt(),
                (24 * density).toInt(),
                (24 * density).toInt()
            )
        }

        root.addView(TextView(this).apply {
            text = "ReVanced Xposed FE Settings"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(colorOnSurface)
            setPadding(0, 0, 0, (6 * density).toInt())
        })

        root.addView(TextView(this).apply {
            text = "Changes apply after Spotify is restarted."
            textSize = 13f
            setTextColor(colorOnSurfaceVariant)
            setPadding(0, 0, 0, (20 * density).toInt())
        })

        val settingsGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(colorSurfaceContainer)
                cornerRadius = 28 * density
            }
            setPadding((18 * density).toInt(), (6 * density).toInt(), (18 * density).toInt(), (6 * density).toInt())
        }

        settingsGroup.addView(createRow("Enable Premium", "Listen in any order, shuffle, or Smart Shuffle", PREF_ENABLE_PREMIUM))
        settingsGroup.addView(createDivider(colorOnSurfaceVariant))
        settingsGroup.addView(createRow("Enable AdBlock", "Block ads and other unwanted content", PREF_ENABLE_ADBLOCK))
        settingsGroup.addView(createDivider(colorOnSurfaceVariant))
        settingsGroup.addView(createRow("Enable Monet Theme by TheWinner02", "Dynamic colors based on the wallpaper", PREF_ENABLE_MONET, false))
        settingsGroup.addView(createDivider(colorOnSurfaceVariant))
        settingsGroup.addView(createRow("Enable RoundyUI by TheWinner02", "Rounded corners on cards and images", PREF_ENABLE_ROUND_UI, false))
        root.addView(settingsGroup)

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, (24 * density).toInt())
        })

        root.addView(MaterialButton(this).apply {
            text = "Open Spotify"
            setTextColor(colorOnPrimary)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            cornerRadius = (24 * density).toInt()
        //    backgroundTintList = android.content.res.ColorStateList.valueOf(colorPrimary)
            layoutParams = LinearLayout.LayoutParams(-1, (52 * density).toInt())
            setOnClickListener { openSpotify() }
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(colorSurface)
            addView(root)
        })

        makePrefsReadable()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun createRow(label: String, subtitle: String, key: String, defaultValue: Boolean = true): LinearLayout {
        val density = resources.displayMetrics.density
        val colorOnSurface = themeColor(com.google.android.material.R.attr.colorOnSurface, Color.WHITE)
        val colorOnSurfaceVariant = themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, "#CAC4D0".toColorInt())
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = (72 * density).toInt()
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            isClickable = true
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        textContainer.addView(TextView(this).apply {
            text = label
            setTextColor(colorOnSurface)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor(colorOnSurfaceVariant)
            textSize = 12f
        })
        row.addView(textContainer)

        val toggle = MaterialSwitch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { view, isChecked ->
                view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }.start()
                prefs.edit(commit = true) { putBoolean(key, isChecked) }
                makePrefsReadable()
            }
        }
        row.setOnClickListener { toggle.isChecked = !toggle.isChecked }
        row.addView(toggle)
        return row
    }

    private fun createDivider(color: Int): View {
        val density = resources.displayMetrics.density
        return View(this).apply {
            alpha = 0.24f
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(-1, (1 * density).toInt())
        }
    }

    private fun themeColor(attr: Int, fallback: Int): Int {
        return MaterialColors.getColor(this, attr, fallback)
    }

    private fun makePrefsReadable() {
        runCatching {
            val prefsFile = File(applicationInfo.dataDir, "shared_prefs/$PREF_FILE.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        }
    }

    private fun openSpotify() {
        val intent = packageManager.getLaunchIntentForPackage("com.spotify.music")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music"))
        startActivity(intent)
    }
}
