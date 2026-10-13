package com.knigdelioglu.puanla

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private var appliedDarkSystemBars: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15+ draws behind system bars. Use the same behavior on older releases.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PuanlaApp(onSystemBarAppearanceChanged = ::updateSystemBarAppearance)
        }
    }

    private fun updateSystemBarAppearance(isDark: Boolean) {
        if (appliedDarkSystemBars == isDark) return
        appliedDarkSystemBars = isDark

        // Respect the app's effective theme, including its manual override.
        // Older Android versions need an opaque navigation bar for button contrast;
        // modern gesture navigation remains transparent.
        val darkNavigationScrim = Color.rgb(27, 27, 30)
        enableEdgeToEdge(
            statusBarStyle = if (isDark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = if (isDark) {
                SystemBarStyle.dark(darkNavigationScrim)
            } else {
                SystemBarStyle.light(Color.rgb(248, 249, 252), darkNavigationScrim)
            }
        )
    }
}
