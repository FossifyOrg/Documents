@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.fossify.commons.compose.system_ui_controller.rememberSystemUiController
import org.fossify.commons.compose.theme.AppThemeSurface
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FontHelper
import java.io.File

@Composable
internal fun DocumentsAppThemeSurface(
    content: @Composable () -> Unit,
) {
    AppThemeSurface {
        MaterialTheme(
            colorScheme = SimpleTheme.colorScheme,
            typography = documentsTypography(),
            shapes = SimpleTheme.shapes,
        ) {
            DocumentsSystemBars()
            content()
        }
    }
}

@Composable
private fun documentsTypography(): Typography {
    val context = LocalContext.current
    val baseTypography = SimpleTheme.typography
    val config = context.baseConfig
    if (config.fontType != FONT_TYPE_CUSTOM) {
        return baseTypography
    }

    val fontFile = File(FontHelper.getFontsDir(context), config.fontName)
    if (!fontFile.exists()) {
        return baseTypography
    }

    val fontFamily = remember(fontFile.path, fontFile.lastModified()) {
        FontFamily(Font(fontFile))
    }
    return remember(baseTypography, fontFamily) {
        baseTypography.withFontFamily(fontFamily)
    }
}

private fun Typography.withFontFamily(fontFamily: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

@Composable
private fun DocumentsSystemBars() {
    val controller = rememberSystemUiController()
    val darkIcons = SimpleTheme.colorScheme.surface.luminance() > DARK_ICON_LUMINANCE_THRESHOLD

    DisposableEffect(controller, darkIcons) {
        controller.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = darkIcons,
        )
        onDispose { }
    }
}

private const val DARK_ICON_LUMINANCE_THRESHOLD = 0.5f
