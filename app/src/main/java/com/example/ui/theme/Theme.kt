package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = OnIndigoPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = VioletSecondaryLight,
    onSecondary = OnVioletSecondaryLight,
    secondaryContainer = VioletContainerLight,
    onSecondaryContainer = OnVioletContainerLight,
    tertiary = MintTertiaryLight,
    onTertiary = OnMintTertiaryLight,
    tertiaryContainer = MintContainerLight,
    onTertiaryContainer = OnMintContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = VioletSecondaryDark,
    onSecondary = OnVioletSecondaryDark,
    secondaryContainer = VioletContainerDark,
    onSecondaryContainer = OnVioletContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = OnMintTertiaryDark,
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = OnMintContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val SpendWiseShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

data class FinanceSemanticColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val isDark: Boolean
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceSemanticColors(
        income = IncomeGreenLight,
        expense = ExpenseRedLight,
        warning = WarningAmberLight,
        isDark = false
    )
}

@Composable
fun SpendWiseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (useDarkTheme) {
        FinanceSemanticColors(
            income = IncomeGreenDark,
            expense = ExpenseRedDark,
            warning = WarningAmberDark,
            isDark = true
        )
    } else {
        FinanceSemanticColors(
            income = IncomeGreenLight,
            expense = ExpenseRedLight,
            warning = WarningAmberLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalFinanceColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpendWiseTypography,
            shapes = SpendWiseShapes,
            content = content
        )
    }
}
