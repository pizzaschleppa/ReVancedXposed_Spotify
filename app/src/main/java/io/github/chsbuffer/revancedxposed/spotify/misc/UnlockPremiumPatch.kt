package io.github.chsbuffer.revancedxposed.spotify.misc

import app.revanced.extension.shared.Logger
import app.revanced.extension.spotify.misc.UnlockPremiumPatch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.revancedxposed.callMethod
import io.github.chsbuffer.revancedxposed.findField
import io.github.chsbuffer.revancedxposed.findFirstFieldByExactType
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun SpotifyHook.UnlockPremium() {

    // --- 1. ATTRIBUTE UNLOCK (CORE PREMIUM) ---
    // Use 'after' to intercept the result.
    // Important: create a copy, do not modify the original object.
    ::productStateProtoFingerprint.hookMethod {
        after { param ->
            val result = param.result as? Map<String, *> ?: return@after
            // Use the standard method you probably already have.
            UnlockPremiumPatch.overrideAttributes(result)
            // To be extra safe, there is no need to reassign param.result
            // because the map was modified internally.
        }
    }

    // --- 2. POPULAR TRACKS (ARTIST PAGE) ---
    ::buildQueryParametersFingerprint.hookMethod {
        after { param ->
            val result = param.result ?: return@after
            val fieldName = "checkDeviceCapability"
            if (result.toString().contains("$fieldName=")) {
                param.result = XposedBridge.invokeOriginalMethod(
                    param.method, param.thisObject, arrayOf(param.args[0], true)
                )
            }
        }
    }

    // --- 3. GOOGLE ASSISTANT (FIX URIs) ---
    ::contextFromJsonFingerprint.hookMethod {
        fun safeRemoveStation(field: Field?, obj: Any?) {
            if (field == null || obj == null) return
            runCatching {
                val value = field.get(obj) as? String ?: return
                field.set(obj, UnlockPremiumPatch.removeStationString(value))
            }
        }

        after { param ->
            val result = param.result ?: return@after
            val clazz = result.javaClass
            safeRemoveStation(clazz.findField("uri"), result)
            safeRemoveStation(clazz.findField("url"), result)
        }
    }

    // --- 4. ANTI-SHUFFLE (GOOGLE ASSISTANT) ---
    runCatching {
        XposedHelpers.findAndHookMethod(
            $$"com.spotify.player.model.command.options.AutoValue_PlayerOptionOverrides$Builder",
            classLoader,
            "build",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.thisObject.callMethod("shufflingContext", false)
                }
            })
    }.onFailure { Logger.printDebug { "PlayerOptionOverrides hook failed: ${it.message}" } }

    // --- 5. CONTEXT MENU CLEANUP (REMOVE ADS) ---
    runCatching {
        val contextMenuViewModelClazz = ::contextMenuViewModelClass.clazz
        XposedBridge.hookAllConstructors(contextMenuViewModelClazz, object : XC_MethodHook() {
            val isPremiumUpsell = runCatching { ::isPremiumUpsellField.field }.getOrNull()

            override fun beforeHookedMethod(param: MethodHookParam) {
                if (isPremiumUpsell == null) return
                val parameterTypes = (param.method as Constructor<*>).parameterTypes
                for (i in param.args.indices) {
                    if (parameterTypes[i].name != "java.util.List") continue
                    val original = param.args[i] as? List<*> ?: continue

                    // Filter out items that lead to Premium ads.
                    val filtered = original.filter { item ->
                        val vm = item?.callMethod("getViewModel")
                        vm?.let { isPremiumUpsell.get(it) as? Boolean } != true
                    }
                    param.args[i] = filtered
                }
            }
        })
    }.onFailure { Logger.printDebug { "ContextMenu hook failed: ${it.message}" } }

    // --- 6. REMOVE AD SECTIONS (HOME & BROWSE) ---
    // For Home.
    ::homeStructureGetSectionsFingerprint.hookMethod {
        after { param ->
            val sections = param.result as? MutableList<*> ?: return@after
            runCatching {
                // Force the list to be mutable (avoids immutable-list errors).
                sections.javaClass.findFirstFieldByExactType(Boolean::class.java).set(sections, true)
                UnlockPremiumPatch.removeHomeSections(sections)
            }
        }
    }

    // For Browse.
    ::browseStructureGetSectionsFingerprint.hookMethod {
        after { param ->
            val sections = param.result as? MutableList<*> ?: return@after
            runCatching {
                // Force the list to be mutable.
                sections.javaClass.findFirstFieldByExactType(Boolean::class.java).set(sections, true)
                UnlockPremiumPatch.removeBrowseSections(sections)
            }
        }
    }

    // --- 7. BLOCK AD POPUPS (PENDRAGON) ---
    // Simulate a natural network error instead of blocking the call.
    val replaceWithRxError = object : XC_MethodHook() {
        val justMethod = DexMethod("Lio/reactivex/rxjava3/core/Single;->just(Ljava/lang/Object;)Lio/reactivex/rxjava3/core/Single;").toMethod()
        val onErrorField = DexField("Lio/reactivex/rxjava3/internal/operators/single/SingleOnErrorReturn;->b:Lio/reactivex/rxjava3/functions/Function;").toField()

        override fun afterHookedMethod(param: MethodHookParam) {
            if (!param.result.javaClass.name.endsWith("SingleOnErrorReturn")) return
            runCatching {
                val errorFunc = onErrorField.get(param.result)
                val applyMethod = errorFunc.javaClass.getMethod("apply", java.lang.Object::class.java)
                val fallbackValue = applyMethod.invoke(errorFunc, Exception("Pendragon block"))
                param.result = justMethod.invoke(null, fallbackValue)
            }
        }
    }

    ::pendragonJsonFetchMessageRequestFingerprint.hookMethod(replaceWithRxError)
    ::pendragonJsonFetchMessageListRequestFingerprint.hookMethod(replaceWithRxError)
}
