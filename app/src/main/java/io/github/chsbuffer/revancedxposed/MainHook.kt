package io.github.chsbuffer.revancedxposed

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.revanced.extension.shared.Utils
import io.github.chsbuffer.revancedxposed.spotify.AdBlockHook
import io.github.chsbuffer.revancedxposed.spotify.RoundyUIHook
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import io.github.chsbuffer.revancedxposed.spotify.ThemeHook
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class MainHook : XposedModule() {
    companion object {
        const val TAG = "ReVancedXposed"

        lateinit var module: MainHook
            private set

        fun log(tag: String, msg: String, tr: Throwable? = null) {
            if (::module.isInitialized) {
                if (tr != null) {
                    module.log(Log.INFO, tag, msg, tr)
                } else {
                    module.log(Log.INFO, tag, msg)
                }
            } else {
                if (tr != null) {
                    Log.e(tag, msg, tr)
                } else {
                    Log.i(tag, msg)
                }
            }
        }
    }

    private val targetPackages = setOf("com.spotify.music")
    private var initialized = false

    fun shouldHook(packageName: String): Boolean {
        return targetPackages.contains(packageName)
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
        log(TAG, "onModuleLoaded: ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        module = this
        if (!param.isFirstPackage) return
        if (!shouldHook(param.packageName)) return

        log(TAG, "onPackageReady: ${param.packageName}")

        val lpparam = PackageParam(
            packageName = param.packageName,
            classLoader = param.classLoader,
            appInfo = param.applicationInfo,
            isFirstPackage = param.isFirstPackage
        )

        val attachMethod = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        hook(attachMethod).intercept { chain ->
            val res = chain.proceed()
            val app = chain.thisObject as? Application
            if (app != null) {
                onApplicationAttached(app, lpparam)
            }
            res
        }
    }

    private fun onApplicationAttached(app: Application, lpparam: PackageParam) {
        if (initialized) return
        initialized = true

        Utils.setContext(app)

        val prefs = getModulePrefs(app)

        if (isReVancedPatched(lpparam)) {
            Utils.showToastLong("ReVanced Xposed FE module does not work with patched app")
            return
        }
        Utils.showToastLong("ReVanced Xposed FE is initializing, please wait...")

        // --- SPOTIFY PATCHES ---
        try {
            SpotifyHook(app, lpparam, prefs.getBoolean(PREF_ENABLE_PREMIUM, true)).Hook()
        } catch (e: Exception) {
            log(TAG, "Spotify patches failed: ${e.message}", e)
        }

        // --- AD BLOCK ---
        try {
            if (prefs.getBoolean(PREF_ENABLE_ADBLOCK, true)) {
                AdBlockHook(lpparam).hook()
                log(TAG, "AdBlocker: Module activated")
            }
        } catch (e: Exception) {
            log(TAG, "AdBlocker failed: ${e.message}", e)
        }

        // --- MONET BLOCK ---
        try {
            if (prefs.getBoolean(PREF_ENABLE_MONET, false)) {
                ThemeHook(app, lpparam).hook()
            }
        } catch (e: Exception) {
            log(TAG, "Monet Mod failed: ${e.message}", e)
        }

        // --- ROUNDY BLOCK ---
        try {
            if (prefs.getBoolean(PREF_ENABLE_ROUND_UI, false)) {
                RoundyUIHook(lpparam).hook()
            }
        } catch (e: Exception) {
            log(TAG, "Roundy Mod failed: ${e.message}", e)
        }
    }

    private fun isReVancedPatched(lpparam: PackageParam): Boolean {
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

    private fun getModulePrefs(app: Application): SharedPreferences {
        return try {
            getRemotePreferences(PREF_FILE)
        } catch (t: Throwable) {
            log(TAG, "Failed to get remote preferences: ${t.message}", t)
            app.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }
    }
}

const val PREF_FILE = "spotify_prefs"
const val PREF_ENABLE_PREMIUM = "enable_premium"
const val PREF_ENABLE_ADBLOCK = "enable_adblock"
const val PREF_ENABLE_MONET = "enable_monet"
const val PREF_ENABLE_ROUND_UI = "enable_round_ui"
