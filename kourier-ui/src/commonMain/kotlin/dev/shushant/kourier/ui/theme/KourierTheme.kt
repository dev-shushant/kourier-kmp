package dev.shushant.kourier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun darkMaterialPalette() = darkColors(
    primary         = KourierDarkColors.statusSuccess,
    primaryVariant  = KourierDarkColors.surfaceElevated,
    secondary       = KourierDarkColors.methodGet,
    background      = KourierDarkColors.background,
    surface         = KourierDarkColors.surface,
    onPrimary       = KourierDarkColors.textPrimary,
    onSecondary     = KourierDarkColors.textPrimary,
    onBackground    = KourierDarkColors.textPrimary,
    onSurface       = KourierDarkColors.textPrimary,
    error           = KourierDarkColors.statusServerError
)

private fun lightMaterialPalette() = lightColors(
    primary         = KourierLightColors.statusSuccess,
    primaryVariant  = KourierLightColors.surfaceElevated,
    secondary       = KourierLightColors.methodGet,
    background      = KourierLightColors.background,
    surface         = KourierLightColors.surface,
    onPrimary       = KourierLightColors.background,
    onSecondary     = KourierLightColors.background,
    onBackground    = KourierLightColors.textPrimary,
    onSurface       = KourierLightColors.textPrimary,
    error           = KourierLightColors.statusServerError
)

@Composable
fun KourierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors      = if (darkTheme) KourierDarkColors else KourierLightColors
    val materialPalette = if (darkTheme) darkMaterialPalette() else lightMaterialPalette()

    CompositionLocalProvider(LocalKourierColors provides colors) {
        MaterialTheme(
            colors  = materialPalette,
            content = content
        )
    }
}
