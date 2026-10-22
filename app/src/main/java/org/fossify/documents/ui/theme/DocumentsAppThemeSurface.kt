package org.fossify.documents.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.fossify.commons.compose.system_ui_controller.rememberSystemUiController
import org.fossify.commons.compose.theme.AppThemeSurface
import org.fossify.commons.compose.theme.SimpleTheme

@Composable
internal fun DocumentsAppThemeSurface(
    content: @Composable () -> Unit,
) {
    AppThemeSurface {
        DocumentsSystemBars()
        content()
    }
}

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
