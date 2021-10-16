package moe.ore.txhook.xposed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XCallback
import moe.ore.txhook.more.logger
import java.lang.Exception

fun Class<*>?.callMethod(funName: String?, vararg args: Any?): Any? {
    try {
        return XposedHelpers.callStaticMethod(this, funName, *args)
    } catch (e: Exception) {
        log(e)
    }
    return null
}

fun callStaticMethod(obj:  Class<*>?, funName: String?, vararg args: Any?): Any? {
    return XposedHelpers.callStaticMethod(obj, funName, *args)
}

fun callMethod(obj: Any?, funName: String?, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(obj, funName, *args)
}

fun Class<*>?.hookMethod(funName: String?): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        XposedBridge.hookAllMethods(this, funName, hook)

        hook
    } catch (e: Exception) {
        log(e)
        e.printStackTrace()
        null
    }
}

fun Class<*>?.hookMethod(funName: String?, vararg args: Class<*>): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        val anise = arrayOfNulls<Any>(args.size + 1)
        args.forEachIndexed { index, clazz ->
            anise[index] = clazz
        }
        anise[anise.size - 1] = hook

        XposedHelpers.findAndHookMethod(this, funName, *anise)

        hook
    } catch (e: Exception) {
        log(e)
        e.printStackTrace()
        null
    }
}

fun hookMethod(clz: String?, loader: ClassLoader?, funName: String?, vararg args: Class<*>): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        val anise = arrayOfNulls<Any>(args.size + 1)
        args.forEachIndexed { index, clazz ->
            anise[index] = clazz
        }
        anise[anise.size - 1] = hook

        XposedHelpers.findAndHookMethod(clz, loader, funName, *anise)

        hook
    } catch (e: Exception) {
        null
    }
}

fun beforeHook(block: (param: XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return object :XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            block(param)
        }
    }
}

fun afterHook(ver: Int = XCallback.PRIORITY_DEFAULT, block: (param: XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return object :XC_MethodHook(ver) {
        override fun afterHookedMethod(param: MethodHookParam) {
            block(param)
        }
    }
}

class XposedMethodHook: XC_MethodHook() {
    private lateinit var beforeFun: XposedMethodHookFunction
    private lateinit var afterFun: XposedMethodHookFunction

    fun before(function: XposedMethodHookFunction): XposedMethodHook {
        this.beforeFun = function
        return this
    }

    fun after(function: XposedMethodHookFunction): XposedMethodHook {
        this.afterFun = function
        return this
    }

    override fun beforeHookedMethod(param: MethodHookParam) {
        if (this::beforeFun.isInitialized) {
            try {
                beforeFun(param)
            } catch (e: Exception) {
                log(e)
            }
        }
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        if (this::afterFun.isInitialized) {
            try {
                afterFun(param)
            } catch (e: Exception) {
                log(e)
            }
        }
    }
}

fun interface XposedMethodHookFunction {
    operator fun invoke(param: XC_MethodHook.MethodHookParam)
}

