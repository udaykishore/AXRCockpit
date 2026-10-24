package com.ukp.axrcockpit.util

import android.util.Log

class Logger private constructor(tag: String) {
    private val tag: String = if (tag.length > 23) tag.substring(0, 23) else tag

    fun v(message: String) {
        Log.v(tag, message)
    }

    fun v(message: String, throwable: Throwable) {
        Log.v(tag, message, throwable)
    }

    fun d(message: String) {
        Log.d(tag, message)
    }

    fun d(message: String, throwable: Throwable) {
        Log.d(tag, message, throwable)
    }

    fun i(message: String) {
        Log.i(tag, message)
    }

    fun i(message: String, throwable: Throwable) {
        Log.i(tag, message, throwable)
    }

    fun w(message: String) {
        Log.w(tag, message)
    }

    fun w(message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    fun e(message: String) {
        Log.e(tag, message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

    companion object {
        private const val GLOBAL_PREFIX = "AXR-"

        @JvmStatic
        fun get(moduleName: String): Logger {
            return Logger("$GLOBAL_PREFIX$moduleName")
        }

        @JvmStatic
        fun get(clazz: Class<*>): Logger {
            return get(clazz.simpleName)
        }
    }
}
