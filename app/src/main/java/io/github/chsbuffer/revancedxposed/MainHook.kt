package io.github.chsbuffer.revancedxposed

import android.app.Application
import android.content.Context
import android.util.Log
import app.revanced.extension.shared.Utils
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
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
        log(TAG, "DIAGNOSTIC MODE — full module (UnlockPremium + NHB), checking if ProductState is reachable")

        // --- SPOTIFY PATCHES (UnlockPremium logs 'overrideAttributes called, map size=N') ---
        try {
            SpotifyHook(app, lpparam).Hook()
            log(TAG, "SpotifyHook applied")
        } catch (e: Exception) {
            log(TAG, "SpotifyHook failed: ${e.message}", e)
        }
    }

}

const val PREF_FILE = "spotify_prefs"
const val PREF_ENABLE_PREMIUM = "enable_premium"
const val PREF_ENABLE_ADBLOCK = "enable_adblock"
const val PREF_ENABLE_MONET = "enable_monet"
const val PREF_ENABLE_ROUND_UI = "enable_round_ui"
