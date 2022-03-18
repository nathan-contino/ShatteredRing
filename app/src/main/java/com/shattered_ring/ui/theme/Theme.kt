package com.shattered_ring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

private val DarkColorPalette = darkColors(
    primary = ShatteredRingGreen,
    primaryVariant = ShatteredRingOtherGreen,
    secondary = ShatteredRingYellow,
)

private val LightColorPalette = lightColors(
    primary = ShatteredRingGreen,
    primaryVariant = ShatteredRingOtherGreen,
    secondary = ShatteredRingYellow

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun ShatteredRingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = {
            ProvideTextStyle(
                value = TextStyle(color = ShatteredRingYellow),
                content = content
            )
        }
    )

}