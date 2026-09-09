package dev.shushant.kourier.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Dark palette (Obsidian Slate — Linear / Raycast / Proxyman aesthetic) ─────
val KourierDarkColors = KourierColorScheme(
    background       = Color(0xFF0C0E12),
    surface          = Color(0xFF12151B),
    surfaceElevated  = Color(0xFF181C24),
    cardBackground   = Color(0xFF14171F),
    border           = Color(0xFF222834),
    divider          = Color(0xFF1A1F29),
    textPrimary      = Color(0xFFF1F3F7),
    textSecondary    = Color(0xFF949BA8),
    textMuted        = Color(0xFF5D6574),
    statusSuccess    = Color(0xFF10B981),
    statusRedirect   = Color(0xFF38BDF8),
    statusClientError= Color(0xFFF59E0B),
    statusServerError= Color(0xFFEF4444),
    statusPending    = Color(0xFFA855F7),
    methodGet        = Color(0xFF38BDF8),
    methodPost       = Color(0xFF34D399),
    methodPut        = Color(0xFFFBBF24),
    methodDelete     = Color(0xFFF87171),
    methodPatch      = Color(0xFFA78BFA),
    methodOther      = Color(0xFF94A3B8),
    codeBackground   = Color(0xFF08090C),
    syntaxKey        = Color(0xFF60A5FA),
    syntaxString     = Color(0xFF93C5FD),
    syntaxNumber     = Color(0xFFFCD34D),
    syntaxBoolean    = Color(0xFFFCA5A5),
    syntaxPunctuation= Color(0xFF64748B),
    isDark           = true
)

// ─── Light palette (Studio Daylight — Linear / Vercel studio aesthetic) ───────
val KourierLightColors = KourierColorScheme(
    background       = Color(0xFFF4F6F9),
    surface          = Color(0xFFFFFFFF),
    surfaceElevated  = Color(0xFFF0F3F7),
    cardBackground   = Color(0xFFFFFFFF),
    border           = Color(0xFFE2E8F0),
    divider          = Color(0xFFEDF2F7),
    textPrimary      = Color(0xFF0F172A),
    textSecondary    = Color(0xFF475569),
    textMuted        = Color(0xFF8E9AA8),
    statusSuccess    = Color(0xFF059669),
    statusRedirect   = Color(0xFF0284C7),
    statusClientError= Color(0xFFD97706),
    statusServerError= Color(0xFFDC2626),
    statusPending    = Color(0xFF7C3AED),
    methodGet        = Color(0xFF0284C7),
    methodPost       = Color(0xFF059669),
    methodPut        = Color(0xFFD97706),
    methodDelete     = Color(0xFFDC2626),
    methodPatch      = Color(0xFF7C3AED),
    methodOther      = Color(0xFF475569),
    codeBackground   = Color(0xFFF8FAFC),
    syntaxKey        = Color(0xFF0369A1),
    syntaxString     = Color(0xFF0F766E),
    syntaxNumber     = Color(0xFFB45309),
    syntaxBoolean    = Color(0xFFBE123C),
    syntaxPunctuation= Color(0xFF64748B),
    isDark           = false
)

// ─── Color scheme data class ──────────────────────────────────────────────────
@Immutable
data class KourierColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBackground: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val statusSuccess: Color,
    val statusRedirect: Color,
    val statusClientError: Color,
    val statusServerError: Color,
    val statusPending: Color,
    val methodGet: Color,
    val methodPost: Color,
    val methodPut: Color,
    val methodDelete: Color,
    val methodPatch: Color,
    val methodOther: Color,
    val codeBackground: Color,
    val syntaxKey: Color,
    val syntaxString: Color,
    val syntaxNumber: Color,
    val syntaxBoolean: Color,
    val syntaxPunctuation: Color,
    val isDark: Boolean
) {
    fun statusColor(code: Int?): Color = when (code) {
        in 200..299 -> statusSuccess
        in 300..399 -> statusRedirect
        in 400..499 -> statusClientError
        in 500..599 -> statusServerError
        else -> textSecondary
    }

    fun methodColor(method: String): Color = when (method.uppercase()) {
        "GET"    -> methodGet
        "POST"   -> methodPost
        "PUT"    -> methodPut
        "DELETE" -> methodDelete
        "PATCH"  -> methodPatch
        else     -> methodOther
    }

    fun methodContainerColor(method: String): Color {
        val base = methodColor(method)
        return if (isDark) base.copy(alpha = 0.14f) else base.copy(alpha = 0.12f)
    }

    fun statusContainerColor(code: Int?): Color {
        val base = statusColor(code)
        return if (isDark) base.copy(alpha = 0.14f) else base.copy(alpha = 0.12f)
    }
}

// ─── CompositionLocal ─────────────────────────────────────────────────────────
val LocalKourierColors = staticCompositionLocalOf<KourierColorScheme> {
    KourierDarkColors
}

// ─── Legacy object alias ──────────────────────────────────────────────────────
object KourierColors {
    val Background        get() = KourierDarkColors.background
    val Surface           get() = KourierDarkColors.surface
    val SurfaceElevated   get() = KourierDarkColors.surfaceElevated
    val CardBackground    get() = KourierDarkColors.cardBackground
    val Border            get() = KourierDarkColors.border
    val Divider           get() = KourierDarkColors.divider
    val TextPrimary       get() = KourierDarkColors.textPrimary
    val TextSecondary     get() = KourierDarkColors.textSecondary
    val TextMuted         get() = KourierDarkColors.textMuted
    val StatusSuccess     get() = KourierDarkColors.statusSuccess
    val StatusRedirect    get() = KourierDarkColors.statusRedirect
    val StatusClientError get() = KourierDarkColors.statusClientError
    val StatusServerError get() = KourierDarkColors.statusServerError
    val StatusPending     get() = KourierDarkColors.statusPending
    val MethodGet         get() = KourierDarkColors.methodGet
    val MethodPost        get() = KourierDarkColors.methodPost
    val MethodPut         get() = KourierDarkColors.methodPut
    val MethodDelete      get() = KourierDarkColors.methodDelete
    val MethodPatch       get() = KourierDarkColors.methodPatch
    val MethodOther       get() = KourierDarkColors.methodOther
    val CodeBackground    get() = KourierDarkColors.codeBackground
    val SyntaxKey         get() = KourierDarkColors.syntaxKey
    val SyntaxString      get() = KourierDarkColors.syntaxString
    val SyntaxNumber      get() = KourierDarkColors.syntaxNumber
    val SyntaxBoolean     get() = KourierDarkColors.syntaxBoolean
    val SyntaxPunctuation get() = KourierDarkColors.syntaxPunctuation
}

