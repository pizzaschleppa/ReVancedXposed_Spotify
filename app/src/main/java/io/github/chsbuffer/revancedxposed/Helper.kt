package io.github.chsbuffer.revancedxposed

import android.content.pm.ApplicationInfo
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * Parameter passed to hook callbacks, providing access to method details,
 * arguments, receiver object, and control over execution and return values.
 */
class HookParam(
    val chain: XposedInterface.Chain,
    val method: Executable = chain.executable,
    var thisObject: Any? = chain.thisObject,
    val args: Array<Any?> = chain.args.toTypedArray()
) {
    var isSkipped: Boolean = false
    var result: Any? = null
        set(value) {
            field = value
            isSkipped = true
        }

    var throwable: Throwable? = null

    fun setResult(result: Any?) {
        this.result = result
        this.isSkipped = true
    }

    fun invokeOriginalMethod(args: Array<Any?> = this.args): Any? {
        val methodObj = method as? Method ?: throw IllegalStateException("Executable is not a Method: $method")
        val invoker = MainHook.module.getInvoker(methodObj).setType(XposedInterface.Invoker.Type.ORIGIN)
        return invoker.invoke(thisObject, *args)
    }
}

typealias MethodHookParam = HookParam
typealias IHookCallback = (HookParam) -> Unit
typealias IScopedHookCallback = ScopedHookParam.(HookParam) -> Unit

open class XC_MethodHook {
    open fun beforeHookedMethod(param: HookParam) {}
    open fun afterHookedMethod(param: HookParam) {}
}

object XC_MethodReplacement {
    fun returnConstant(value: Any?): XC_MethodHook {
        return object : XC_MethodHook() {
            override fun beforeHookedMethod(param: HookParam) {
                param.result = value
            }
        }
    }
}

class HookDsl<TCallback>(emptyCallback: TCallback) {
    var before: TCallback = emptyCallback
    var after: TCallback = emptyCallback

    fun before(f: TCallback) {
        this.before = f
    }

    fun after(f: TCallback) {
        this.after = f
    }
}

fun Executable.hookMethod(block: HookDsl<IHookCallback>.() -> Unit): XposedInterface.HookHandle {
    val builder = HookDsl<IHookCallback> {}.apply(block)
    return hookMethodInternal(this, builder.before, builder.after)
}

fun Executable.hookMethod(callback: XC_MethodHook): XposedInterface.HookHandle {
    return hookMethodInternal(this, { callback.beforeHookedMethod(it) }, { callback.afterHookedMethod(it) })
}

fun Member.hookMethod(block: HookDsl<IHookCallback>.() -> Unit): XposedInterface.HookHandle {
    val executable = this as? Executable ?: throw IllegalArgumentException("Member is not an Executable: $this")
    return executable.hookMethod(block)
}

fun Member.hookMethod(callback: XC_MethodHook): XposedInterface.HookHandle {
    val executable = this as? Executable ?: throw IllegalArgumentException("Member is not an Executable: $this")
    return executable.hookMethod(callback)
}

fun hookMethodInternal(
    executable: Executable,
    before: IHookCallback,
    after: IHookCallback
): XposedInterface.HookHandle {
    return MainHook.module.hook(executable).intercept { chain ->
        val param = HookParam(chain)

        try {
            before(param)
        } catch (t: Throwable) {
            MainHook.log("HookEngine", "Error in before hook for ${executable.name}", t)
        }

        if (param.isSkipped) {
            return@intercept param.result
        }

        var argsModified = false
        val origArgs = chain.args
        if (param.args.size == origArgs.size) {
            for (i in param.args.indices) {
                if (param.args[i] !== origArgs[i]) {
                    argsModified = true
                    break
                }
            }
        } else {
            argsModified = true
        }

        val res = try {
            if (param.thisObject !== chain.thisObject && param.thisObject != null) {
                if (argsModified) {
                    chain.proceedWith(param.thisObject!!, param.args)
                } else {
                    chain.proceedWith(param.thisObject!!)
                }
            } else {
                if (argsModified) {
                    chain.proceed(param.args)
                } else {
                    chain.proceed()
                }
            }
        } catch (t: Throwable) {
            param.throwable = t
            null
        }

        // Set result for after callback without triggering isSkipped
        param.isSkipped = false
        param.result = res

        try {
            after(param)
        } catch (t: Throwable) {
            MainHook.log("HookEngine", "Error in after hook for ${executable.name}", t)
        }

        if (param.throwable != null && param.result === res) {
            throw param.throwable!!
        }

        param.result
    }
}

object XposedBridge {
    fun log(msg: String) {
        MainHook.log("ReVancedXposed", msg)
    }

    fun log(tr: Throwable) {
        MainHook.log("ReVancedXposed", tr.message ?: "Exception", tr)
    }

    fun hookMethod(member: Member, callback: XC_MethodHook): XposedInterface.HookHandle {
        return (member as Executable).hookMethod(callback)
    }

    fun hookMethod(member: Member, block: HookDsl<IHookCallback>.() -> Unit): XposedInterface.HookHandle {
        return (member as Executable).hookMethod(block)
    }

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: XC_MethodHook): List<XposedInterface.HookHandle> {
        val handles = mutableListOf<XposedInterface.HookHandle>()
        for (m in clazz.declaredMethods) {
            if (m.name == methodName) {
                handles.add(m.hookMethod(callback))
            }
        }
        return handles
    }

    fun hookAllMethods(clazz: Class<*>, methodName: String, block: HookDsl<IHookCallback>.() -> Unit): List<XposedInterface.HookHandle> {
        val handles = mutableListOf<XposedInterface.HookHandle>()
        for (m in clazz.declaredMethods) {
            if (m.name == methodName) {
                handles.add(m.hookMethod(block))
            }
        }
        return handles
    }

    fun hookAllConstructors(clazz: Class<*>, callback: XC_MethodHook): List<XposedInterface.HookHandle> {
        val handles = mutableListOf<XposedInterface.HookHandle>()
        for (c in clazz.declaredConstructors) {
            handles.add(c.hookMethod(callback))
        }
        return handles
    }

    fun hookAllConstructors(clazz: Class<*>, block: HookDsl<IHookCallback>.() -> Unit): List<XposedInterface.HookHandle> {
        val handles = mutableListOf<XposedInterface.HookHandle>()
        for (c in clazz.declaredConstructors) {
            handles.add(c.hookMethod(block))
        }
        return handles
    }

    fun invokeOriginalMethod(method: Member, thisObject: Any?, args: Array<Any?>?): Any? {
        val methodObj = method as? Method ?: throw IllegalStateException("Member is not a Method: $method")
        val invoker = MainHook.module.getInvoker(methodObj).setType(XposedInterface.Invoker.Type.ORIGIN)
        return if (args == null || args.isEmpty()) {
            invoker.invoke(thisObject)
        } else {
            invoker.invoke(thisObject, *args)
        }
    }
}

@JvmInline
value class ScopedHookParam(val outerParam: MethodHookParam)

fun scopedHook(vararg pairs: Pair<Member, HookDsl<IScopedHookCallback>.() -> Unit>): XC_MethodHook {
    val hook = ScopedHook()
    pairs.forEach { (member, block) ->
        val builder = HookDsl<IScopedHookCallback> {}.apply(block)
        hook.hookInnerMethod(member, builder.before, builder.after)
    }
    return hook
}

inline fun scopedHook(
    hookMethod: Member, crossinline f: HookDsl<IScopedHookCallback>.() -> Unit
): XC_MethodHook {
    val hook = ScopedHook()
    val builder = HookDsl<IScopedHookCallback> {}.apply(f)
    hook.hookInnerMethod(hookMethod, builder.before, builder.after)
    return hook
}

class ScopedHook : XC_MethodHook() {
    inline fun hookInnerMethod(
        hookMethod: Member,
        crossinline before: IScopedHookCallback,
        crossinline after: IScopedHookCallback
    ) {
        (hookMethod as Executable).hookMethod {
            before { param ->
                val outerParam = outerParam.get() ?: return@before
                before(ScopedHookParam(outerParam), param)
            }
            after { param ->
                val outerParam = outerParam.get() ?: return@after
                after(ScopedHookParam(outerParam), param)
            }
        }
    }

    val outerParam: ThreadLocal<HookParam> = ThreadLocal<HookParam>()

    override fun beforeHookedMethod(param: MethodHookParam) {
        outerParam.set(param)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        outerParam.remove()
    }
}

/**
 * Compatible LoadPackageParam representation for LibXposed callbacks.
 */
data class PackageParam(
    val packageName: String,
    val classLoader: ClassLoader,
    val appInfo: ApplicationInfo,
    val isFirstPackage: Boolean = true
) {
    val isFirstApplication get() = isFirstPackage
}

typealias LoadPackageParam = PackageParam

fun injectHostClassLoaderToSelf(self: ClassLoader, host: ClassLoader) {
    val findClassMethod =
        XposedHelpers.findMethodExact(ClassLoader::class.java, "findClass", String::class.java)
    self.setObjectField("parent", object : ClassLoader(self.parent) {
        override fun findClass(name: String): Class<*> {
            try {
                return findClassMethod(self, name) as Class<*>
            } catch (_: InvocationTargetException) {
            }

            try {
                return host.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }

            throw ClassNotFoundException(name)
        }
    })
}

@Suppress("UNCHECKED_CAST")
fun Class<*>.enumValueOf(name: String): Enum<*>? {
    return try {
        java.lang.Enum.valueOf(this as Class<out Enum<*>>, name)
    } catch (_: IllegalArgumentException) {
        null
    }
}