package io.github.chsbuffer.revancedxposed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.revanced.extension.shared.Utils
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
    val hooksByPackage = mapOf(
        "com.spotify.music" to { SpotifyHook(app, lpparam) },
    )

    fun shouldHook(packageName: String): Boolean {
        if (!hooksByPackage.containsKey(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return
        if (!shouldHook(lpparam.packageName)) return
        this.lpparam = lpparam

        // --- NEW TRIGGER: LONG PRESS ON PROFILE ICON ---
        XposedHelpers.findAndHookMethod(
            "android.app.Activity",
            lpparam.classLoader,
            "onPostCreate", // Use onPostCreate to make sure the UI is ready
            android.os.Bundle::class.java,
            object : XC_MethodHook() {
                @SuppressLint("DiscouragedApi")
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    if (!activity.javaClass.name.contains("MainActivity")) return

                    // Spotify loads the avatar asynchronously, so wait until the view is laid out.
                    val decorView = activity.window.decorView as ViewGroup
                    decorView.viewTreeObserver.addOnGlobalLayoutListener {
                        // Try to find the avatar using common IDs.
                        val avatarIds = listOf("profile_button", "profile_image", "avatar", "user_avatar", "faceview", "faceheader_image")
                        var found = false

                        for (idName in avatarIds) {
                            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
                            if (resId != 0) {
                                val avatarView = activity.findViewById<View>(resId)
                                if (avatarView != null && !found) {
                                    setModLongClickListener(avatarView, activity)
                                    found = true
                                }
                            }
                        }

                        // If the ID is not found, look for the first ImageView in the top-left corner.
                        if (!found) {
                            findAvatarRecursive(decorView, activity)
                        }
                    }
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

            // --- PREMIUM BLOCK ---
            // This is now isolated: if Roundy crashes above, this will still run.
            try {
                if (prefs.getBoolean("enable_premium", true)) {
                    hooksByPackage[lpparam.packageName]?.invoke()?.Hook()
                }
            } catch (e: Exception) {
                XposedBridge.log("Mod Premium fallita: ${e.message}")
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

    // Set the listener and provide feedback.
    private fun setModLongClickListener(view: View, activity: Activity) {
        if (view.tag == "mod_hooked") return
        view.tag = "mod_hooked"

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

    // Find the profile image by its position (top-left).
    private fun findAvatarRecursive(view: View, activity: Activity) {
        if (view is ImageView || view.contentDescription?.toString()?.contains("Profilo", true) == true) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            // The avatar is usually within the first 150px from the top and 150px from the left.
            if (location[0] < 150 && location[1] < 200 && view.width > 0) {
                setModLongClickListener(view, activity)
                return
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAvatarRecursive(view.getChildAt(i), activity)
            }
        }
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

fun inContext(lpparam: LoadPackageParam, f: (Application) -> Unit) {
    val appClazz = XposedHelpers.findClass(lpparam.appInfo.className, lpparam.classLoader)
    XposedBridge.hookMethod(appClazz.getMethod("onCreate"), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val app = param.thisObject as Application
            Utils.setContext(app)
            f(app)
        }
    })
}
