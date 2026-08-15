package io.github.chsbuffer.revancedxposed

import android.app.Application
import android.content.Context
import app.revanced.extension.shared.Utils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.revancedxposed.spotify.AdBlockHook
import io.github.chsbuffer.revancedxposed.spotify.RoundyUIHook
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import io.github.chsbuffer.revancedxposed.spotify.ThemeHook

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    lateinit var startupParam: StartupParam
    lateinit var lpparam: LoadPackageParam
    lateinit var app: Application
    var targetPackageName: String? = null
    private val targetPackages = setOf("com.spotify.music")

    fun shouldHook(packageName: String): Boolean {
        if (!targetPackages.contains(packageName)) return false
        if (targetPackageName == null) targetPackageName = packageName
        return targetPackageName == packageName
    }
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!lpparam.isFirstApplication) return
        if (!shouldHook(lpparam.packageName)) return
        this.lpparam = lpparam

        inContext(lpparam) { app ->
            this.app = app

            val prefs = getModulePrefs()

            if (isReVancedPatched(lpparam)) {
                Utils.showToastLong("ReVanced Xposed FE module does not work with patched app")
                return@inContext
            }
            Utils.showToastLong("ReVanced Xposed FE is initializing, please wait...")

            // --- SPOTIFY PATCHES ---
            try {
                SpotifyHook(app, lpparam, prefs.getBoolean(PREF_ENABLE_PREMIUM, true)).Hook()
            } catch (e: Exception) {
                XposedBridge.log("Spotify patches failed: ${e.message}")
            }

            // --- AD BLOCK ---
            try {
                if (prefs.getBoolean(PREF_ENABLE_ADBLOCK, true)) {
                    AdBlockHook(lpparam).hook()
                    XposedBridge.log("AdBlocker: Modulo attivato")
                }
            } catch (e: Exception) {
                XposedBridge.log("AdBlocker fallito: ${e.message}")
            }

            // --- MONET BLOCK ---
            try {
                if (prefs.getBoolean(PREF_ENABLE_MONET, true)) {
                    ThemeHook(app, lpparam).hook()
                }
            } catch (e: Exception) {
                XposedBridge.log("Mod Monet fallita: ${e.message}")
            }

            // --- ROUNDY BLOCK (the main suspect) ---
            try {
                if (prefs.getBoolean(PREF_ENABLE_ROUND_UI, true)) {
                    RoundyUIHook(lpparam).hook()
                }
            } catch (e: Exception) {
                XposedBridge.log("Mod Roundy fallita: ${e.message}")
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

    private fun getModulePrefs(): XSharedPreferences {
        return XSharedPreferences(BuildConfig.APPLICATION_ID, PREF_FILE).apply {
            makeWorldReadable()
            reload()
        }
    }
}

const val PREF_FILE = "spotify_prefs"
const val PREF_ENABLE_PREMIUM = "enable_premium"
const val PREF_ENABLE_ADBLOCK = "enable_adblock"
const val PREF_ENABLE_MONET = "enable_monet"
const val PREF_ENABLE_ROUND_UI = "enable_round_ui"

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
