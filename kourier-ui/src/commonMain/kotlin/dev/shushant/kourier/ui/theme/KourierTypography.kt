package dev.shushant.kourier.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KourierTypography {
    // Font families
    val SansSerif  = FontFamily.SansSerif   // System UI font — for labels, titles, body
    val Monospace  = FontFamily.Monospace   // Code, URLs, header values, paths

    // ─── UI text styles (SansSerif) ───────────────────────────────────────
    val headline = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        letterSpacing = (-0.3).sp
    )

    val titleLarge = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 15.sp,
        letterSpacing = (-0.2).sp
    )

    val title = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp
    )

    val subtitle = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
        letterSpacing = 0.2.sp
    )

    val body = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 18.sp
    )

    val bodySmall = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp
    )

    val caption = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp
    )

    val label = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 0.3.sp
    )

    // Small all-caps badge label — HTTP method, status code
    val badge = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 10.5.sp,
        letterSpacing = 0.4.sp
    )

    val badgeSmall = TextStyle(
        fontFamily = SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 9.5.sp,
        letterSpacing = 0.2.sp
    )

    // ─── Code / technical text styles (Monospace) ─────────────────────────
    val code = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 18.sp
    )

    val codeBold = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
        lineHeight = 18.sp
    )

    val codeSmall = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        lineHeight = 16.sp
    )

    // URL / path display — monospace but slightly larger
    val urlText = TextStyle(
        fontFamily = Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.5.sp,
        lineHeight = 17.sp
    )
}
