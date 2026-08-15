package io.github.chsbuffer.revancedxposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.revanced.extension.shared.Utils
import java.util.Collections
import java.util.WeakHashMap
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.revancedxposed.spotify.AdBlockHook
import io.github.chsbuffer.revancedxposed.spotify.RoundyUIHook
import io.github.chsbuffer.revancedxposed.spotify.SettingsSheet
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import io.github.chsbuffer.revancedxposed.spotify.ThemeHook
import androidx.core.view.isNotEmpty

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    lateinit var startupParam: StartupParam
    lateinit var lpparam: LoadPackageParam
    lateinit var app: Application
    var targetPackageName: String? = null
    private val targetPackages = setOf("com.spotify.music")
    private val hookedMenuViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    fun shouldHook(packageName: String): Boolean {
        if (!targetPackages.contains(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return
        if (!shouldHook(lpparam.packageName)) return
        this.lpparam = lpparam

        // --- MODULE MENU TRIGGER: LONG PRESS ON PROFILE ICON ---
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "onPostCreate",
            android.os.Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    installMenuTriggerWhenReady(activity)
                }
            }
        )

        inContext(lpparam) { app ->
            this.app = app

            // Load preferences once.
            val prefs = app.getSharedPreferences("spotify_prefs", 0)

            if (isReVancedPatched(lpparam)) {
                Utils.showToastLong("ReVanced Xposed FE module does not work with patched app")
                return@inContext
            }
            Utils.showToastLong("ReVanced Xposed FE is initializing, please wait...")

            // --- SPOTIFY PATCHES ---
            try {
                SpotifyHook(app, lpparam, prefs.getBoolean("enable_premium", true)).Hook()
            } catch (e: Exception) {
                XposedBridge.log("Spotify patches failed: ${e.message}")
            }

            // --- AD BLOCK ---
            try {
                // You can add "enable_adblock" to SettingsSheet later.
                if (prefs.getBoolean("enable_adblock", true)) {
                    AdBlockHook(lpparam).hook()
                    XposedBridge.log("AdBlocker: Modulo attivato")
                }
            } catch (e: Exception) {
                XposedBridge.log("AdBlocker fallito: ${e.message}")
            }

            // --- MONET BLOCK ---
            try {
                if (prefs.getBoolean("enable_monet", true)) {
                    ThemeHook(app, lpparam).hook()
                }
            } catch (e: Exception) {
                XposedBridge.log("Mod Monet fallita: ${e.message}")
            }

            // --- ROUNDY BLOCK (the main suspect) ---
            try {
                if (prefs.getBoolean("enable_round_ui", true)) {
                    RoundyUIHook(lpparam).hook()
                }
            } catch (e: Exception) {
                XposedBridge.log("Mod Roundy fallita: ${e.message}")
            }
            
        }
    }

    private fun installMenuTriggerWhenReady(activity: Activity, attempt: Int = 0) {
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        decorView.postDelayed({
            val avatarView = findAvatarView(activity, decorView)
            if (avatarView != null) {
                setModLongClickListener(avatarView, activity)
                return@postDelayed
            }

            if (attempt < MENU_TRIGGER_MAX_ATTEMPTS && !activity.isFinishing && !activity.isDestroyed) {
                installMenuTriggerWhenReady(activity, attempt + 1)
            }
        }, MENU_TRIGGER_RETRY_DELAY_MS)
    }

    // Set the listener and provide feedback.
    private fun setModLongClickListener(view: View, activity: Activity) {
        if (!hookedMenuViews.add(view)) return

        view.setOnLongClickListener {
            // If the clicked view is a container (ViewGroup), look for the image inside it.
            val realView = if (it is ViewGroup && it.isNotEmpty()) {
                it.getChildAt(0)
            } else {
                it
            }

            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            SettingsSheet.show(activity, realView)
            true
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun findAvatarView(activity: Activity, decorView: ViewGroup): View? {
        val avatarIds = listOf(
            "profile_button",
            "profile_image",
            "profile_picture",
            "avatar",
            "user_avatar",
            "faceview",
            "faceheader_image"
        )

        for (idName in avatarIds) {
            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
            if (resId == 0) continue
            val view = activity.findViewById<View>(resId)
            if (view?.isShown == true && view.width > 0 && view.height > 0) return view
        }

        return findAvatarRecursive(decorView, activity)
    }

    // Find the profile image by semantic hints first, then by its usual toolbar position.
    private fun findAvatarRecursive(view: View, activity: Activity): View? {
        if (isLikelyAvatar(view, activity)) return view

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAvatarRecursive(view.getChildAt(i), activity)?.let { return it }
            }
        }

        return null
    }

    private fun isLikelyAvatar(view: View, activity: Activity): Boolean {
        if (!view.isShown || view.width <= 0 || view.height <= 0) return false

        val description = view.contentDescription?.toString().orEmpty()
        val className = view.javaClass.name.lowercase()
        val resourceName = runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrDefault("").lowercase()

        val hasAvatarHint = listOf(
            "profile",
            "profilo",
            "account",
            "avatar",
            "face"
        ).any { hint ->
            description.contains(hint, ignoreCase = true) ||
                    resourceName.contains(hint) ||
                    className.contains(hint)
        }
        if (hasAvatarHint) return true

        if (view !is ImageView) return false

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val density = activity.resources.displayMetrics.density
        val maxX = (96 * density).toInt()
        val maxY = (160 * density).toInt()
        val minSize = (24 * density).toInt()
        val maxSize = (80 * density).toInt()

        return location[0] in 0..maxX &&
                location[1] in 0..maxY &&
                view.width in minSize..maxSize &&
                view.height in minSize..maxSize
    }

    private fun isReVancedPatched(lpparam: LoadPackageParam): Boolean {
        return runCatching {
            lpparam.classLoader.loadClass("app.revanced.extension.shared.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.extension.shared.utils.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.Utils")
        }.isSuccess || runCatching {
            lpparam.classLoader.loadClass("app.revanced.integrations.shared.utils.Utils")
        }.isSuccess
    }

    override fun initZygote(startupParam: StartupParam) {
        this.startupParam = startupParam
        XposedInit = startupParam
    }
}

private const val MENU_TRIGGER_MAX_ATTEMPTS = 12
private const val MENU_TRIGGER_RETRY_DELAY_MS = 250L

fun inContext(lpparam: LoadPackageParam, f: (Application) -> Unit) {
    XposedHelpers.findAndHookMethod(
        Application::class.java,
        "attach",
        Context::class.java,
        object : XC_MethodHook() {
            private var initialized = false

            override fun afterHookedMethod(param: MethodHookParam) {
                if (initialized) return
                initialized = true

                val app = param.thisObject as Application
                Utils.setContext(app)
                f(app)
            }
        }
    )
}
