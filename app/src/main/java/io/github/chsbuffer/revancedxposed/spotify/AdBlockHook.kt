package io.github.chsbuffer.revancedxposed.spotify

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class AdBlockHook(private val lpparam: LoadPackageParam) {

    fun hook() {
        val cl = lpparam.classLoader
        XposedBridge.log("RE-VANCED XPOSED: Starting AdBlocker (Refined)")

        // ==========================================
        // 1. FLAG PATCH (legacy/compat)
        // ==========================================
        // Note: In recent versions, 'LoadedFlags' is often obfuscated.
        // The main logic now lives in ProductStateProto (UnlockPremiumPatch).
        // This hook acts as an extra safeguard for specific variants.
        runCatching {
            val flagsClass = cl.loadClass("com.spotify.connectivity.flags.LoadedFlags")
            XposedBridge.hookAllMethods(flagsClass, "get", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = runCatching { XposedHelpers.getObjectField(param.args[0], "identifier") as? String }.getOrNull()
                    if (key == "ads") {
                        param.result = false
                    }
                }
            })
            XposedBridge.log("AdBlocker: Hook set on LoadedFlags")
        }.onFailure {
            XposedBridge.log("AdBlocker: Hook failed on LoadedFlags - ${it.message}")
        }

        // ==========================================
        // 2. DISABLE AD SETTINGS
        // ==========================================
        runCatching {
            // Try to find known classes that manage ad state.
            val adsClass = cl.loadClass("com.spotify.adsinternal.adscore.AdsSettings")
            XposedBridge.hookAllMethods(adsClass, "isAdsEnabled", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            })
            XposedBridge.log("AdBlocker: Hook set on AdsSettings")
        }.onFailure {
            XposedBridge.log("AdBlocker: Hook failed on AdsSettings - ${it.message}")
        }

        // ==========================================
        // 3. HIDE AD UI COMPONENTS
        // ==========================================
        runCatching {
            val countdownView = cl.loadClass("com.spotify.adsinternal.playback.video.CountdownBarView")
            XposedHelpers.findAndHookMethod(countdownView, "onMeasure", Int::class.java, Int::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "setMeasuredDimension", 0, 0)
                    param.result = null
                }
            })
            XposedBridge.log("AdBlocker: Hook set on CountdownBarView (Hiding)")
        }.onFailure {
            XposedBridge.log("AdBlocker: Hook failed on CountdownBarView - ${it.message}")
        }
    }
}
