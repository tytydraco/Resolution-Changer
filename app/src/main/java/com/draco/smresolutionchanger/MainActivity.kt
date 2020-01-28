package com.draco.smresolutionchanger

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.concurrent.fixedRateTimer
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    /* Command that user must run to grant permissions */
    private val adbCommand = "pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"

    /* Preferences */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /* Internal classes */
    private lateinit var wmApi: WmApi

    /* UI elements */
    private lateinit var width: EditText
    private lateinit var height: EditText
    private lateinit var density: EditText
    private lateinit var reset: Button
    private lateinit var apply: Button

    object DefaultScreenSpecs {
        var width: Int = 0
        var height: Int = 0
        var density: Int = 0
        var diagInches: Int = 0
        var diagPixels: Int = 0
    }

    /* Set DefaultScreenSpecs to current settings */
    private fun setupDefaultScreenSpecs() {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        DefaultScreenSpecs.width = dm.widthPixels
        DefaultScreenSpecs.height = dm.heightPixels
        DefaultScreenSpecs.density = dm.densityDpi

        val wi = dm.widthPixels.toDouble() / dm.xdpi.toDouble()
        val hi = dm.heightPixels.toDouble() / dm.ydpi.toDouble()

        DefaultScreenSpecs.diagInches = sqrt(wi.pow(2.0) + hi.pow(2.0)).toInt()
        DefaultScreenSpecs.diagPixels = sqrt(dm.widthPixels.toDouble().pow(2) +
                dm.heightPixels.toDouble().pow(2)).toInt()
    }

    /* Estimate proper DPI for device */
    private fun calculateDPI(x: Int, y: Int): Int {
        val diagPixels = sqrt(x.toDouble().pow(2) + y.toDouble().pow(2)).toInt()
        return (diagPixels / DefaultScreenSpecs.diagInches)
    }

    /* Apply resolution and density */
    private fun apply() {
        var w = 0
        var h = 0
        var d = 0

        w = try {
            Integer.parseInt(width.text.toString())
        } catch (_: Exception) {
            DefaultScreenSpecs.width
        }

        h = try {
            Integer.parseInt(height.text.toString())
        } catch (_: Exception) {
            DefaultScreenSpecs.height
        }

        d = try {
            Integer.parseInt(density.text.toString())
        } catch (_: Exception) {
            calculateDPI(w, h)
        }

        wmApi.setBypassBlacklist(true)
        wmApi.setDisplayResolution(w, h)
        wmApi.setDisplayDensity(d)

        Handler().postDelayed({
            showWarningDialog()
        }, 500)

        updateEditTexts()
    }

    /* Show 5 second countdown */
    private fun showWarningDialog() {
        var dismissed = false

        val dialog = AlertDialog.Builder(this)
                .setTitle("Confirm Settings")
                .setMessage("Resetting in 5 seconds.")
                .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                    dismissed = true
                }
                .setCancelable(false)
                .create()

        dialog.show()

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (dismissed) {
                    this.cancel()
                } else
                    dialog.setMessage("Resetting in " + ((millisUntilFinished / 1000) + 1) + " seconds.")
            }

            override fun onFinish() {
                if (!dismissed) {
                    dialog.dismiss()
                    reset()
                }
            }
        }.start()
    }

    /* Use new resolution for text */
    private fun updateEditTexts() {
        /* Read screen specs */
        setupDefaultScreenSpecs()

        width.setText(DefaultScreenSpecs.width.toString())
        height.setText(DefaultScreenSpecs.height.toString())
        density.setText(DefaultScreenSpecs.density.toString())
    }

    /* Reset resolution and density */
    private fun reset() {
        wmApi.setBypassBlacklist(true)
        wmApi.clearDisplayDensity()
        wmApi.clearDisplayResolution()

        updateEditTexts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Configure classes for our activity */
        wmApi = WmApi(contentResolver)

        /* Setup preferences */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        editor = sharedPreferences.edit()

        /* Setup UI elements */
        width = findViewById(R.id.width)
        height = findViewById(R.id.height)
        density = findViewById(R.id.density)
        reset = findViewById(R.id.reset)
        apply = findViewById(R.id.apply)

        reset.setOnClickListener {
            reset()

            reset.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press))
        }

        apply.setOnClickListener {
            apply()

            apply.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press))
        }

        /* Request or confirm if we can perform proper commands */
        checkPermissions()

        /* Show the current display config */
        updateEditTexts()
    }

    private fun hasPermissions(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissions() {
        if (hasPermissions())
            return

        val dialog = AlertDialog.Builder(this)
                .setTitle("Missing Permissions")
                .setMessage(getString(R.string.adb_tutorial) + "adb shell $adbCommand")
                .setPositiveButton("Check Again", null)
                .setNeutralButton("Setup ADB", null)
                .setCancelable(false)
                .create()

        dialog.setOnShowListener {
            /* We don't dismiss on Check Again unless we actually have the permission */
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (hasPermissions())
                    dialog.dismiss()
            }

            /* Open tutorial but do not dismiss until user presses Check Again */
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                val uri = Uri.parse("https://www.xda-developers.com/install-adb-windows-macos-linux/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }

        dialog.show()

        /* Check every second if the permission was granted */
        fixedRateTimer("permissionCheck", false, 0, 1000) {
            if (hasPermissions()) {
                dialog.dismiss()
                this.cancel()
            }
        }
    }
}
