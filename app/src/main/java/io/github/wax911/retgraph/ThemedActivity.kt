package io.github.wax911.retgraph

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class ThemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        super.onCreate(savedInstanceState)
    }
}