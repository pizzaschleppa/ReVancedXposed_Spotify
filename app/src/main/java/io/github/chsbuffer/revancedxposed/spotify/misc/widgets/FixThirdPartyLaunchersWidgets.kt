package io.github.chsbuffer.revancedxposed.spotify.misc.widgets

import io.github.chsbuffer.revancedxposed.XC_MethodReplacement
import io.github.chsbuffer.revancedxposed.XposedBridge
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook

fun SpotifyHook.FixThirdPartyLaunchersWidgets() {
    runCatching {
        ::canBindAppWidgetPermissionFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
    }.onFailure { XposedBridge.log("FixWidgets: canBindAppWidget hook failed: ${it.message}") }
}