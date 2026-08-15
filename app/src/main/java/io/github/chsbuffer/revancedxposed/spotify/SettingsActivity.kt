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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
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
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        window.statusBarColor = "#121212".toColorInt()
        window.navigationBarColor = "#121212".toColorInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor("#121212".toColorInt())
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
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (6 * density).toInt())
        })

        root.addView(TextView(this).apply {
            text = "Changes apply after Spotify is restarted."
            textSize = 13f
            setTextColor("#A0A0A0".toColorInt())
            setPadding(0, 0, 0, (20 * density).toInt())
        })

        root.addView(createRow("Enable Premium", "Listen in any order, shuffle, or Smart Shuffle", PREF_ENABLE_PREMIUM))
        root.addView(createRow("Enable AdBlock", "Block ads and other unwanted content", PREF_ENABLE_ADBLOCK))
        root.addView(createRow("Enable Monet Theme", "Dynamic colors based on the wallpaper", PREF_ENABLE_MONET))
        root.addView(createRow("Enable RoundyUI", "Rounded corners on cards and images", PREF_ENABLE_ROUND_UI))

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, (24 * density).toInt())
        })

        root.addView(Button(this).apply {
            text = "Open Spotify"
            setTextColor(Color.BLACK)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor("#1DB954".toColorInt())
                cornerRadius = 100f
            }
            layoutParams = LinearLayout.LayoutParams(-1, (48 * density).toInt())
            setOnClickListener { openSpotify() }
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor("#121212".toColorInt())
            addView(root)
        })

        makePrefsReadable()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun createRow(label: String, subtitle: String, key: String): LinearLayout {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
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
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor("#A0A0A0".toColorInt())
            textSize = 12f
        })
        row.addView(textContainer)

        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(key, true)
            val spotifyGreen = "#1DB954".toColorInt()
            thumbDrawable?.setTint(if (isChecked) spotifyGreen else Color.GRAY)
            scaleX = 1.25f
            scaleY = 1.25f
            setOnCheckedChangeListener { view, isChecked ->
                view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                    view.animate()
                        .scaleX(1.25f)
                        .scaleY(1.25f)
                        .setDuration(150)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }.start()
                prefs.edit(commit = true) { putBoolean(key, isChecked) }
                thumbDrawable?.setTint(if (isChecked) spotifyGreen else Color.GRAY)
                makePrefsReadable()
            }
        }
        row.setOnClickListener { toggle.isChecked = !toggle.isChecked }
        row.addView(toggle)
        return row
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
