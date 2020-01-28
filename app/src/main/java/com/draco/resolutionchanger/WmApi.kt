package com.draco.resolutionchanger

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.Settings
import android.view.Display
import java.lang.Exception

class WmApi(private val contentResolver: ContentResolver) {

    /* User ID to pass to WindowManager APIs */
    private val userId = -3

    /* List of Global settings that allow blacklisted APIs to be called */
    private val blacklistGlobalSettings = listOf(
            "hidden_api_policy",
            "hidden_api_policy_pre_p_apps",
            "hidden_api_policy_p_apps"
    )

    /* If true, allow blacklisted APIs to be called */
    fun setBypassBlacklist(mode: Boolean) {
        when (mode) {
            true -> {
                for (setting in blacklistGlobalSettings) {
                    Settings.Global.putInt(contentResolver, setting, 1)
                }
            }
            false -> {
                for (setting in blacklistGlobalSettings) {
                    Settings.Global.putInt(contentResolver, setting, 0)
                }
            }
        }
    }

    /* Fetch the system WindowManager service */
    @SuppressLint("PrivateApi")
    private fun getWindowManagerService(): Any? {
        var wmService: Any? = null

        try {
            wmService = Class.forName("android.view.WindowManagerGlobal")
                    .getMethod("getWindowManagerService")
                    .invoke(null)
        } catch (_: Exception) {}

        return wmService
    }

    /* Set display resolution */
    @SuppressLint("PrivateApi")
    fun setDisplayResolution(x: Int, y: Int) {
        /* Return if we can't get WindowManager service */
        val wmService = getWindowManagerService() ?: return

        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("setForcedDisplaySize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY, x, y)
        } catch (_: Exception) {}
    }

    /* Clear display resolution */
    @SuppressLint("PrivateApi")
    fun clearDisplayResolution() {
        /* Return if we can't get WindowManager service */
        val wmService = getWindowManagerService() ?: return

        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("clearForcedDisplaySize", Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY)
        } catch (_: Exception) {}
    }

    /* Set display density */
    @SuppressLint("PrivateApi")
    fun setDisplayDensity(density: Int) {
        /* Return if we can't get WindowManager service */
        val wmService = getWindowManagerService() ?: return

        /* Try the old API for some devices */
        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("setForcedDisplayDensity", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY, density)
        } catch (_: Exception) {}

        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("setForcedDisplayDensityForUser", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY, density, userId)
        } catch (_: Exception) {}
    }

    /* Clear display density */
    @SuppressLint("PrivateApi")
    fun clearDisplayDensity() {
        /* Return if we can't get WindowManager service */
        val wmService = getWindowManagerService() ?: return

        /* Try the old API for some devices */
        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("clearForcedDisplayDensity", Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY)
        } catch (_: Exception) {}

        try {
            Class.forName("android.view.IWindowManager")
                    .getMethod("clearForcedDisplayDensityForUser", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(wmService, Display.DEFAULT_DISPLAY, userId)
        } catch (_: Exception) {}
    }
}