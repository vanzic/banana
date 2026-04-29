package com.vixcy.banana.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.vixcy.banana.ui.Banana

private val BananaColorScheme = lightColorScheme(
    primary       = Banana.Color.Accent,
    onPrimary     = Banana.Color.Surface,
    secondary     = Banana.Color.Lavender,
    background    = Banana.Color.Bg,
    onBackground  = Banana.Color.Ink,
    surface       = Banana.Color.Surface,
    onSurface     = Banana.Color.Ink,
    error         = Banana.Color.Error,
    onError       = Banana.Color.Surface,
    outline       = Banana.Color.Stroke
)

@Composable
fun BananaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge — content draws under status/navigation bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = Banana.Color.Bg.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = BananaColorScheme,
        typography  = Typography,
        content     = content
    )
}
