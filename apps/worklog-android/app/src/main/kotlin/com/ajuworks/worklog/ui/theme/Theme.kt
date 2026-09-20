package com.ajuworks.worklog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A calm blue-green: office-adjacent without looking like enterprise software,
// and legible against both surfaces.
private val Teal = Color(0xFF00696E)
private val TealLight = Color(0xFF4FD8DF)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F7),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365),
    tertiary = Color(0xFF4F5F7E),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF1F7),
    secondary = Color(0xFFB1CBCD),
    tertiary = Color(0xFFB7C7EA),
    error = Color(0xFFFFB4AB),
)

/** The elapsed-time readout needs to be legible at arm's length. */
private val WorkLogTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun WorkLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = WorkLogTypography,
        content = content,
    )
}
