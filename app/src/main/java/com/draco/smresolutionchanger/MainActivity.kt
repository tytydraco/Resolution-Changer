package com.draco.smresolutionchanger

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText

lateinit var sharedPrefs: SharedPreferences
lateinit var editor: SharedPreferences.Editor
lateinit var width: EditText
lateinit var height: EditText
lateinit var apply: Button
lateinit var def: Button
lateinit var manualRefresh: Button
lateinit var density: EditText


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences("SMResolutionChanger", Context.MODE_PRIVATE)
        editor = sharedPrefs.edit()

        width = findViewById(R.id.width)
        height = findViewById(R.id.height)
        apply = findViewById(R.id.apply)
        def = findViewById(R.id.def)
        manualRefresh = findViewById(R.id.manual_refresh)
        density = findViewById(R.id.density)

        apply.setOnClickListener { applyResolution(this, getTextResolution(), getTextDensity()) }
        def.setOnClickListener { defaultResolution(this) }
        manualRefresh.setOnClickListener { refreshViews(this) }

        permissionCheck(this)
    }

    private fun getTextDensity(): String {
        return density.text.toString()
    }

    private fun getTextResolution(): String {
        return width.text.toString() + "x" + height.text.toString()
    }
}
