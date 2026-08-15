package io.github.chsbuffer.revancedxposed.spotify

import android.content.res.Resources
import android.graphics.Outline
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class RoundyUIHook(private val lpparam: XC_LoadPackage.LoadPackageParam) {

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Resources.getSystem().displayMetrics
        )
    }

    private val radiusLarge = dpToPx(28f)
    private val radiusFull = dpToPx(100f)
    private val radiusThreshold = dpToPx(15f)

    fun hook() {
        val classLoader = lpparam.classLoader

        /*
        // 0. Universal hook with debug logging
        XposedHelpers.findAndHookMethod(
            "android.view.View",
            classLoader,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View

                    // DEBUG: Extract the ID so we know what we are touching.
                    val resName = try { view.resources.getResourceEntryName(view.id) } catch (_: Exception) { "null" }
                    if (view is ImageView || resName != "null") {
                        Log.d(TAG, "View detected: ID -> $resName | Class -> ${view.javaClass.simpleName}")
                    }

                    applyRoundingIfTarget(view)
                }
            }
        )
         */

        // 1. Universal hook for rounding based on ID and class.
        XposedHelpers.findAndHookMethod(
            "android.view.View",
            classLoader,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View
                    applyRoundingIfTarget(view)
                }
            }
        )

        // 2. ImageView-specific hook (playlist covers).
        // onAttachedToWindow is often not enough when the image is recycled in a list.
        XposedHelpers.findAndHookMethod(
            "android.widget.ImageView",
            classLoader,
            "setImageDrawable",
            android.graphics.drawable.Drawable::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val imageView = param.thisObject as ImageView
                    applyRoundingIfTarget(imageView)
                }
            }
        )

        // 3. GradientDrawable hook (buttons).
        XposedHelpers.findAndHookMethod(
            "android.graphics.drawable.GradientDrawable",
            classLoader,
            "setCornerRadius",
            Float::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val original = param.args[0] as Float
                    param.args[0] = if (original > radiusThreshold) radiusFull else radiusLarge
                }
            }
        )

        // 4. BottomSheet hook (the container that slides up from the bottom).
        XposedHelpers.findAndHookMethod(
            "com.google.android.material.bottomsheet.BottomSheetBehavior",
            classLoader,
            "onLayoutChild",
            "androidx.coordinatorlayout.widget.CoordinatorLayout",
            View::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.args[1] as View

                    // Apply rounding only to the TOP corners (top left and top right).
                    // This is typical for Material 3 BottomSheets.
                    view.clipToOutline = true
                    view.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            // Create a rectangle that extends below the view so the bottom corners stay square.
                            outline.setRoundRect(
                                0, 0,
                                view.width, view.height + radiusLarge.toInt(),
                                radiusLarge
                            )
                        }
                    }
                }
            }
        )

// 5. Backup hook for BottomSheet backgrounds.
// Many apps use MaterialShapeDrawable to handle panel corners.
        XposedHelpers.findAndHookMethod(
            "com.google.android.material.shape.MaterialShapeDrawable",
            classLoader,
            "setInterpolation",
            Float::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // If the radius is set in code, force it to our radiusLarge.
                    XposedHelpers.callMethod(param.thisObject, "setCornerSize", radiusLarge)
                }
            }
        )
    }

    private fun applyRoundingIfTarget(view: View) {
        val resName = try { view.resources.getResourceEntryName(view.id) } catch (_: Exception) { "" }
        val className = view.javaClass.name.lowercase()

        // 1. Image detection (track and playlist covers).
        // Use the ImageView class check so we do not miss anything.
        val isAvatar = className.contains("faceview") || resName.contains("face")
        val isImage = (view is ImageView || className.contains("imageview")) && !isAvatar

        // 2. Header and cover detection (the large cover at the top).
        val isCoverOrHeader = resName.contains("header") ||
                resName.contains("cover") ||
                resName.contains("art") ||
                resName.contains("entity")

        // 3. Sheet and card detection.
        val isSheet = className.contains("bottomsheet") || resName.contains("sheet") || resName.contains("queue")
        val isCard = className.contains("card") || resName.contains("tile")
        val isSearchBar = resName == "browse_search_bar_container" || resName.contains("search")
        val isCat = resName == "seek_frame" || resName.contains("seek")

        // 4. Row filter (to avoid clipping text in the library or playlist).
        // If it is a container (Layout), but NOT an image and NOT a sheet.
        val isTextContainerRow = (resName.contains("row") || resName.contains("item")) && !isImage && !isSheet

        // Selection logic.
        val shouldRound = when {
            isAvatar -> false         // Profile.
            isImage -> true           // All images, small and large.
            isCoverOrHeader -> true   // Main radio/playlist cover.
            isSheet -> true           // Menu and queue.
            isCard -> true            // Card Home/Search
            isSearchBar -> true      // Search bar.
            isCat -> true            //
            else -> false
        }

        if (shouldRound) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (isSheet) {
                        // Top corners only for panels.
                        outline.setRoundRect(0, 0, view.width, view.height + radiusLarge.toInt(), radiusLarge)
                    } else {
                        // Full rounding for all covers (tracks and headers).
                        outline.setRoundRect(0, 0, view.width, view.height, radiusLarge)
                    }
                }
            }
        }
    }
}
