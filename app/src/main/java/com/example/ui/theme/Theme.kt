package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.core.localization.AppStrings
import com.example.core.localization.EnglishStrings
import com.example.core.localization.BanglaStrings
import com.example.core.model.AppLanguage

private val LuxurySalonColorScheme = darkColorScheme(
    primary = BurnishedGold,
    onPrimary = EmeraldDeep,
    primaryContainer = AntiqueBrass,
    onPrimaryContainer = IvoryStock,
    secondary = ChampagneGold,
    onSecondary = EmeraldDeep,
    secondaryContainer = EmeraldElevated,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = SuccessEmerald,
    onTertiary = EmeraldDeep,
    background = ObsidianNight,
    onBackground = TextPrimaryLight,
    surface = ObsidianCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = EmeraldSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1F312A),
    error = ErrorCrimson,
    onError = Color.White
)

private val HighContrastSalonColorScheme = darkColorScheme(
    primary = Color(0xFFFFDF73),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFB3861F),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7BFFB9),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF101513),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A2923),
    onSurfaceVariant = Color(0xFFE0EAE4),
    outline = Color(0xFFFFDF73),
    error = Color(0xFFFF6B6B),
    onError = Color.Black
)

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
val LocalTablePalette = staticCompositionLocalOf { TablePalettes.first() }

@Composable
fun WorldCardMasterTheme(
    language: AppLanguage = AppLanguage.ENGLISH,
    tableThemeId: String = "classic_green",
    highContrast: Boolean = false,
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val strings = if (language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings
    val palette = tablePaletteById(tableThemeId)
    val colorScheme = if (highContrast) HighContrastSalonColorScheme else LuxurySalonColorScheme
    val typography = createAppTypography(isBengali = language == AppLanguage.BANGLA, scale = textScale)

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalTablePalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
