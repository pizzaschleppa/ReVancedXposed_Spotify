package io.github.chsbuffer.revancedxposed.spotify

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Color
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import androidx.core.graphics.toColorInt
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class ThemeHook(app: Application, private val lpparam: XC_LoadPackage.LoadPackageParam) {

    private val colorCache = ConcurrentHashMap<Int, Int>()
    private val res = app.resources

    // --- MONET'S COLORS ---
    @SuppressLint("DiscouragedApi")
    private val primaryBg = try {
        app.getColor(res.getIdentifier("system_neutral1_900", "color", "android"))
    } catch (_: Exception) { Color.BLACK }

    @SuppressLint("DiscouragedApi")
    private val secondaryBg = try {
        app.getColor(res.getIdentifier("system_neutral1_800", "color", "android"))
    } catch (_: Exception) {
        "#121212".toColorInt() }

    @SuppressLint("DiscouragedApi")
    private val accent = try {
        app.getColor(res.getIdentifier("system_accent1_200", "color", "android"))
    } catch (_: Exception) {
        "#1DB954".toColorInt() }

    // NEW: Value for the “Pressed” status
    @SuppressLint("DiscouragedApi")
    private val accentPressed = try {
        app.getColor(res.getIdentifier("system_accent1_400", "color", "android"))
    } catch (_: Exception) {
        "#1ABC54".toColorInt() }

    fun hook() {
        val classLoader = lpparam.classLoader

        // 1. PorterDuffColorFilter (Remains unchanged; this is fine)
        XposedHelpers.findAndHookConstructor(
            "android.graphics.PorterDuffColorFilter",
            classLoader,
            Int::class.javaPrimitiveType,
            android.graphics.PorterDuff.Mode::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = replaceColorLogic(param.args[0] as Int)
                }
            }
        )

        // 2. ColorStateList: Surgical Management of States (Pressed/Selected)
        XposedHelpers.findAndHookMethod(
            "android.content.res.ColorStateList",
            classLoader,
            "getColorForState",
            IntArray::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val states = param.args[0] as IntArray
                    val originalColor = param.result as Int

                    val isPressed = states.contains(android.R.attr.state_pressed)
                    val isSelected = states.contains(android.R.attr.state_selected)
                    val isFocused = states.contains(android.R.attr.state_focused)

                    param.result = when {
                        // WHEN PRESSED: Apply the “accentPressed” color with 30% opacity
                        // This creates a “stained glass” effect on the covers
                        isPressed || isFocused -> {
                            Color.argb(
                                77, // Fixed 30% alpha (about 77/255).
                                Color.red(accentPressed),
                                Color.green(accentPressed),
                                Color.blue(accentPressed)
                            )
                        }

                        // IF SELECTED (e.g., NavBar icon active): Full accent
                        isSelected -> accent

                        // OTHERWISE: Standard logic
                        else -> replaceColorLogic(originalColor)
                    }
                }
            }
        )

        // 3. Color.parseColor
        XposedHelpers.findAndHookMethod(
            "android.graphics.Color",
            classLoader,
            "parseColor",
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = replaceColorLogic(param.result as Int)
                }
            }
        )

        // 4. Paint.setColor
        XposedHelpers.findAndHookMethod(
            "android.graphics.Paint",
            classLoader,
            "setColor",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = replaceColorLogic(param.args[0] as Int)
                }
            }
        )

        // 5. GradientDrawable
        XposedHelpers.findAndHookMethod(
            "android.graphics.drawable.GradientDrawable",
            classLoader,
            "setColors",
            IntArray::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val colors = param.args[0] as IntArray
                    for (i in colors.indices) colors[i] = replaceColorLogic(colors[i])
                }
            }
        )
        // 6. Resource Hook (getColor)
        // Intercept every time Spotify requests a color via an ID (e.g., R.color.spotify_green)
        XposedHelpers.findAndHookMethod(
            "android.content.res.Resources",
            lpparam.classLoader,
            "getColor",
            Int::class.javaPrimitiveType,
            "android.content.res.Resources.Theme",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val color = param.result as Int
                    param.result = replaceColorLogic(color)
                }
            }
        )
        /*
        // 7. Resource hook (getColorStateList)
        // Important for switches and icons in settings.
        XposedHelpers.findAndHookMethod(
            "android.content.res.Resources",
            lpparam.classLoader,
            "getColorStateList",
            Int::class.javaPrimitiveType,
            "android.content.res.Resources.Theme",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val csl = param.result as? android.content.res.ColorStateList ?: return

                    // Create a new ColorStateList based on our Monet logic.
                    // This forces switches and clickable text to follow the theme.
                    param.result = android.content.res.ColorStateList.valueOf(replaceColorLogic(csl.defaultColor))
                }
            }
        )
        */
        // 8. Hooks for TypedArrays (The “Final Blow” for XML)
        // When Android reads an attribute from an XML theme (e.g., ?attr/colorPrimary)
        XposedHelpers.findAndHookMethod(
            "android.content.res.TypedArray",
            lpparam.classLoader,
            "getColor",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val color = param.result as Int
                    param.result = replaceColorLogic(color)
                }
            }
        )

        // 9.  REMOVING SHADING/SHADOWS
        XposedHelpers.findAndHookMethod(
            "android.view.View",
            classLoader,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View

                    // We retrieve the ID name (e.g., “shadow,” “fade_overlay”)
                    val resName = try {
                        view.resources.getResourceEntryName(view.id).lowercase()
                    } catch (_: Exception) { "" }

                    // Let's identify the suspicious shadows
                    // Spotify often uses “shadow,” “edge_fade,” or similar names for those overlays
                    val isShadowOrFade = resName.contains("shadow") ||
                            resName.contains("fade") ||
                            resName.contains("gradient")

                    if (isShadowOrFade && view.javaClass.name == "android.view.View") {
                        // Let's hide the view by setting it to GONE
                        view.visibility = View.GONE

                        // Optional: Set the dimensions to 0 just to be safe
                        view.layoutParams?.let {
                            it.width = 0
                            it.height = 0
                        }
                    }

                    // Bonus: Let's Remove the Fading Edge from Lists (RecyclerView)
                    if (view.javaClass.name.contains("RecyclerView")) {
                        view.isHorizontalFadingEdgeEnabled = false
                        view.isVerticalFadingEdgeEnabled = false
                    }
                }
            }
        )
    }

    private fun replaceColorLogic(color: Int): Int {
        if (color == Color.TRANSPARENT) return 0
        colorCache[color]?.let { return it }

        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val newColor = when {
            // 1. SPOTIFY GREEN -> ACCENT
            (g > 100 && g > r * 1.1 && g > b * 1.1) -> accent

            // 2. TEXT AND LIGHT-UP ICONS -> ACCENT
            // (Comment this out if you prefer white text to stay white instead of being colored.)
            (r > 150 && abs(r - g) < 20 && abs(r - b) < 20) -> accent

            // 3. BASIC BACKGROUND (Deep Black) -> PRIMARY BG
            // Spotify uses very low RGB values (e.g., 18, 18, 18) for the background behind everything.
            (r <= 25 && g <= 25 && b <= 25) -> primaryBg

            // 4. HIGH AREAS AND INACTIVE BUTTONS -> SECONDARY BG
            // This is where the chip fills live! (e.g., 36,36,36 or 42,42,42)
            (r in 26..70 && g in 26..70 && b in 26..70) -> secondaryBg

            // 5. MEDIUM AND LIGHT GREYS -> Keep the original
            // Avoid coloring everything; otherwise, secondary text and separators lose their meaning.
            else -> color
        }

        val finalColor = Color.argb(a, Color.red(newColor), Color.green(newColor), Color.blue(newColor))
        colorCache[color] = finalColor
        return finalColor
    }
}
