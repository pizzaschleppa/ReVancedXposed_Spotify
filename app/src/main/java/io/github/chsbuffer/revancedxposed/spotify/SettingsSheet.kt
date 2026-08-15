package io.github.chsbuffer.revancedxposed.spotify

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.ViewTreeObserver
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.edit
import java.io.File
import kotlin.system.exitProcess
import androidx.core.graphics.toColorInt

object SettingsSheet {

    // The function now also accepts anchorView (the avatar view).
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    fun show(activity: Activity, anchorView: View?) {
        val prefs = activity.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
        val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        val density = activity.resources.displayMetrics.density

        // Initial state: invisible, tiny, and transparent for the animation.
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor("#202020".toColorInt())
                val radius = (28 * density)
                // Rounded corners on every side for a floating card.
                cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
            }
            val p = (24 * density).toInt()
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins((20 * density).toInt(), 0, (20 * density).toInt(), 0)
            }
            isClickable = true
            elevation = 40f

            // --- INITIAL ANIMATION STATE ---
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
        }

        // --- TITLE ---
        root.addView(TextView(activity).apply {
            text = "Revanced Xposed FE Settings"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (15 * density).toInt())
        })

        // --- OPTIONS ---
        root.addView(createRow(activity, "Enable Premium", "Listen in any order, shuffle, or Smart Shuffle", "enable_premium", prefs))
        root.addView(createRow(activity, "Enable AdBlock", "Block ads and other unwanted content", "enable_adblock", prefs))
        root.addView(createRow(activity, "Enable Momnet Theme", "Dynamic colors based on the wallpaper", "enable_monet", prefs))
        root.addView(createRow(activity, "Enable RoundyUI", "Rounded corners on cards and images", "enable_round_ui", prefs))

        // --- SPACING ---
        root.addView(View(activity).apply { layoutParams = LinearLayout.LayoutParams(-1, (20 * density).toInt()) })

        // --- RESTART BUTTON ---
        val restartButton = Button(activity).apply {
            text = "Apply and Restart"
            setTextColor(Color.BLACK)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor("#1DB954".toColorInt())
                cornerRadius = 100f
            }
            elevation = 15f
            layoutParams = LinearLayout.LayoutParams((180 * density).toInt(), (45 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setOnClickListener {
                val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                exitProcess(0)
            }
        }
        root.addView(restartButton)

        // --- WRAPPER AND ANIMATION ---
        val wrapper = object : LinearLayout(activity) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // --- SHRINK ANIMATION FOR CLOSING (toward the avatar) ---
                    // The pivot is already set, so it will shrink toward the point it came from.
                    root.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction { dialog.dismiss() }
                        .start()
                }
                return true
            }
        }.apply {
            // Center it to make the expansion and shrink feel like a card animation.
            gravity = Gravity.CENTER
            setBackgroundColor("#B3000000".toColorInt())
            addView(root)
        }

        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                root.viewTreeObserver.removeOnPreDrawListener(this)

                if (anchorView != null) {
                    val rect = android.graphics.Rect()
                    anchorView.getGlobalVisibleRect(rect)

                    val rootLoc = IntArray(2)
                    root.getLocationOnScreen(rootLoc)

                    // --- FINE TUNING ---
                    // Increase these values if the animation is still too far left or too high.
                    val offsetX = 180 * density // Move the origin point to the RIGHT.
                    val offsetY = 180 * density // Move the origin point DOWN.

                    // Calculate the visual center and apply the offsets.
                    val visualCenterX = rect.left + (rect.width() / 2f) + offsetX
                    val visualCenterY = rect.top + (rect.height() / 2f) + offsetY

                    // Pivot relative to the sheet.
                    root.pivotX = visualCenterX - rootLoc[0]
                    root.pivotY = visualCenterY - rootLoc[1]
                } else {
                    root.pivotX = root.width / 2f
                    root.pivotY = root.height / 2f
                }

                // Start the animation.
                root.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(1000)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()

                return true
            }
        })

        dialog.setContentView(wrapper)
        dialog.window?.apply {
            setLayout(-1, -1) // Full screen
            // Disable the standard Dialog animations so we can use our own.
            setWindowAnimations(0)
        }
        dialog.show()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetWorldReadable")
    private fun createRow(context: Context, label: String, subtitle: String, key: String, prefs: android.content.SharedPreferences): LinearLayout {
        val density = context.resources.displayMetrics.density
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
            isClickable = true
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        textContainer.addView(TextView(context).apply {
            text = label; setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD)
        })
        textContainer.addView(TextView(context).apply {
            text = subtitle; setTextColor("#A0A0A0".toColorInt()); textSize = 12f
        })
        row.addView(textContainer)
        val toggle = Switch(context).apply {
            isChecked = prefs.getBoolean(key, true)
            val spotifyGreen = "#1DB954".toColorInt()
            thumbDrawable?.setTint(if (isChecked) spotifyGreen else Color.GRAY)
            scaleX = 1.50f
            scaleY = 1.50f
            setOnCheckedChangeListener { view, isChecked ->
                view.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
                    view.animate().scaleX(1.50f).scaleY(1.50f).setDuration(150).setInterpolator(OvershootInterpolator(2f)).start()
                }.start()
                prefs.edit(commit = true) { putBoolean(key, isChecked) }
                thumbDrawable?.setTint(if (isChecked) spotifyGreen else Color.GRAY)
                runCatching {
                    val file = File(context.applicationInfo.dataDir, "shared_prefs/spotify_prefs.xml")
                    if (file.exists()) file.setReadable(true, false)
                }
            }
        }
        row.setOnClickListener {
            it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(OvershootInterpolator(1.5f)).start()
            }.start()
            toggle.isChecked = !toggle.isChecked
        }
        row.addView(toggle)
        return row
    }
}
