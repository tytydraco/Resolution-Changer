package com.draco.smresolutionchanger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.Display
import android.widget.Toast
import java.util.*

const val sizeName = "display_size_forced"
const val densityName = "display_density_forced"

fun permissionCheck(context: Context): Boolean {
    val permissionCheck = ContextCompat.checkSelfPermission(context,
            Manifest.permission.WRITE_SECURE_SETTINGS)
    if (permissionCheck == PackageManager.PERMISSION_DENIED) {
        val error = AlertDialog.Builder(context)
        error.setTitle("Missing Permissions")
        error.setMessage("To allow this app to work, you must run an ADB command via your computer.\n\nadb shell pm grant " + context.packageName + " android.permission.WRITE_SECURE_SETTINGS")
        error.setPositiveButton("Ok") { _, _ -> permissionCheck(context) }
        error.setNegativeButton("Close") { _, _ -> System.exit(0) }
        error.setCancelable(false)
        error.show()
    } else {
        afterPermissionCheck(context)
        return true
    }
    return false
}

fun afterPermissionCheck(context: Context) {
    val firstLaunch = sharedPrefs.getBoolean("firstLaunch", true)
    if (firstLaunch) {
        defaultResolution(context)
    }
    refreshViews(context)
}

fun defaultResolution(context: Context) {

    val firstLaunch = sharedPrefs.getBoolean("firstLaunch", true)
    if (firstLaunch) {
        val sizeSetting = Settings.Global.getString(context.contentResolver, sizeName)
        val densitySetting = Settings.Secure.getString(context.contentResolver, densityName)
        if (sizeSetting == null || densitySetting == null) {
            editor.putInt("defWidth", 1080)
            editor.putInt("defHeight", 1920)
            editor.putInt("defDensity", 480)
            editor.putInt("setWidth", 1080)
            editor.putInt("setHeight", 1920)
            editor.putInt("setDensity", 480)
            Toast.makeText(context, "Assuming standard 1080x1920.", Toast.LENGTH_SHORT).show()
        } else if (!sizeSetting.contains(",")) {
            editor.putInt("defWidth", 1080)
            editor.putInt("defHeight", 1920)
            editor.putInt("defDensity", 480)
            editor.putInt("setWidth", 1080)
            editor.putInt("setHeight", 1920)
            editor.putInt("setDensity", 480)
            editor.apply()
            Toast.makeText(context, "Assuming standard 1080x1920.", Toast.LENGTH_SHORT).show()
        } else {
            val settingArr = sizeSetting.split(",")
            editor.putInt("defWidth", settingArr[0].toInt())
            editor.putInt("defHeight", settingArr[1].toInt())
            editor.putInt("defDensity", densitySetting.toInt())
            editor.putInt("setWidth", settingArr[0].toInt())
            editor.putInt("setHeight", settingArr[1].toInt())
            editor.putInt("setDensity", densitySetting.toInt())
        }
        editor.putBoolean("firstLaunch", false)
        editor.apply()
    } else {
        val width = sharedPrefs.getInt("defWidth", 1080)
        val height = sharedPrefs.getInt("defHeight", 1920)
        val density = sharedPrefs.getInt("defDensity", 480)
        applyResolution(context, width.toString() + "x" + height.toString(), density.toString())
    }
}

fun applyResolution(context: Context, r: String, d: String) {
    val rParts = r.split("x")
    editor.putInt("setDensity", d.toInt())
    editor.putInt("setWidth", rParts[0].toInt())
    editor.putInt("setHeight", rParts[1].toInt())
    editor.apply()

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
        wmDensityNew(d)
    } else {
        wmDensity(d)
    }

    wmSize(r)
    refreshViews(context)
}

fun refreshViews(context: Context) {
    val sizeSetting = sharedPrefs.getInt("setWidth", 1080).toString() + "x" + sharedPrefs.getInt("setHeight", 1920).toString()
    val densitySetting = Settings.Secure.getString(context.contentResolver, densityName)
    val r = sizeSetting.split("x")
    width.setText(r[0])
    height.setText(r[1])
    density.setText(densitySetting)

}

@SuppressLint("PrivateApi")
@Throws(Exception::class)
private fun getWindowManagerService(): Any {
    return Class.forName("android.view.WindowManagerGlobal")
            .getMethod("getWindowManagerService")
            .invoke(null)
}

@SuppressLint("PrivateApi")
@Throws(Exception::class)
private fun wmSize(commandArg: String) {
    if (commandArg == "reset") {
        Class.forName("android.view.IWindowManager")
                .getMethod("clearForcedDisplaySize", Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY)
    } else {
        val scanner = Scanner(commandArg)
        scanner.useDelimiter("x")

        val width = scanner.nextInt()
        val height = scanner.nextInt()

        scanner.close()

        Class.forName("android.view.IWindowManager")
                .getMethod("setForcedDisplaySize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, width, height)
    }
}

@SuppressLint("PrivateApi")
@Throws(Exception::class)
fun wmDensity(commandArg: String) {
    if (commandArg == "reset") {
        Class.forName("android.view.IWindowManager")
                .getMethod("clearForcedDisplayDensity", Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY)
    } else {
        val density = Integer.parseInt(commandArg)

        Class.forName("android.view.IWindowManager")
                .getMethod("setForcedDisplayDensity", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, density)
    }
}

@SuppressLint("PrivateApi")
@Throws(Exception::class)
private fun wmDensityNew(commandArg: String) {
    // From android.os.UserHandle
    val USER_CURRENT_OR_SELF = -3

    if (commandArg == "reset") {
        Class.forName("android.view.IWindowManager")
                .getMethod("clearForcedDisplayDensityForUser", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, USER_CURRENT_OR_SELF)
    } else {
        val density = Integer.parseInt(commandArg)

        Class.forName("android.view.IWindowManager")
                .getMethod("setForcedDisplayDensityForUser", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(getWindowManagerService(), Display.DEFAULT_DISPLAY, density, USER_CURRENT_OR_SELF)
    }
}
