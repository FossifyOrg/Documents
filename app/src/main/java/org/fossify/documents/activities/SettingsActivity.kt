package org.fossify.documents.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fossify.commons.activities.BaseComposeActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.documents.extensions.config
import org.fossify.documents.extensions.launchChangeAppLanguageIntent
import org.fossify.documents.extensions.startCustomizationActivity
import org.fossify.documents.ui.screens.SettingsScreen
import org.fossify.documents.ui.theme.DocumentsAppThemeSurface
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : BaseComposeActivity() {

    private val preferences by lazy { config }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val wasUseEnglishToggledFlow by preferences.wasUseEnglishToggledFlow
                    .collectAsStateWithLifecycle(preferences.wasUseEnglishToggled)
                val useEnglishFlow by preferences.useEnglishFlow
                    .collectAsStateWithLifecycle(preferences.useEnglish)
                val showCheckmarksOnSwitches by preferences.showCheckmarksOnSwitchesFlow
                    .collectAsStateWithLifecycle(preferences.showCheckmarksOnSwitches)
                val rememberPdfPage by preferences.rememberPdfPageFlow
                    .collectAsStateWithLifecycle(preferences.rememberPdfPage)
                val showFileLocations by preferences.showFileLocationsFlow
                    .collectAsStateWithLifecycle(preferences.showFileLocations)
                val displayLanguage = remember { Locale.getDefault().displayLanguage }
                val isUseEnglishEnabled by remember(wasUseEnglishToggledFlow) {
                    derivedStateOf {
                        (wasUseEnglishToggledFlow || Locale.getDefault().language != "en") && !isTiramisuPlus()
                    }
                }

                SettingsScreen(
                    displayLanguage = displayLanguage,
                    isUseEnglishEnabled = isUseEnglishEnabled,
                    isUseEnglishChecked = useEnglishFlow,
                    isShowingCheckmarksOnSwitches = showCheckmarksOnSwitches,
                    rememberPdfPage = rememberPdfPage,
                    showFileLocations = showFileLocations,
                    onUseEnglishPress = { isChecked ->
                        preferences.useEnglish = isChecked
                        exitProcess(0)
                    },
                    onSetupLanguagePress = ::launchChangeAppLanguageIntent,
                    onRememberPdfPageChange = { checked ->
                        preferences.rememberPdfPage = checked
                    },
                    onShowFileLocationsChange = { checked ->
                        preferences.showFileLocations = checked
                    },
                    customizeColors = ::startCustomizationActivity,
                    goBack = ::finish
                )
            }
        }
    }
}
