@file:Suppress("FunctionNaming", "LongParameterList", "UnusedPrivateMember")

package org.fossify.documents.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fossify.commons.compose.extensions.MyDevices
import org.fossify.commons.compose.lists.SimpleColumnScaffold
import org.fossify.commons.compose.settings.SettingsGroup
import org.fossify.commons.compose.settings.SettingsHorizontalDivider
import org.fossify.commons.compose.settings.SettingsPreferenceComponent
import org.fossify.commons.compose.settings.SettingsSwitchComponent
import org.fossify.commons.compose.settings.SettingsTitleTextComponent
import org.fossify.commons.compose.theme.AppThemeSurface
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.documents.R

@Composable
internal fun SettingsScreen(
    displayLanguage: String,
    isUseEnglishEnabled: Boolean,
    isUseEnglishChecked: Boolean,
    isShowingCheckmarksOnSwitches: Boolean,
    rememberPdfPage: Boolean,
    showFileLocations: Boolean,
    onUseEnglishPress: (Boolean) -> Unit,
    onSetupLanguagePress: () -> Unit,
    onRememberPdfPageChange: (Boolean) -> Unit,
    onShowFileLocationsChange: (Boolean) -> Unit,
    customizeColors: () -> Unit,
    goBack: () -> Unit,
) {
    SimpleColumnScaffold(title = stringResource(id = org.fossify.commons.R.string.settings), goBack = goBack) {
        SettingsGroup(title = {
            SettingsTitleTextComponent(text = stringResource(id = org.fossify.commons.R.string.color_customization))
        }) {
            SettingsPreferenceComponent(
                label = stringResource(id = org.fossify.commons.R.string.customize_colors),
                doOnPreferenceClick = customizeColors,
            )
        }

        if (isUseEnglishEnabled || isTiramisuPlus()) {
            SettingsHorizontalDivider()
            SettingsGroup(title = {
                SettingsTitleTextComponent(text = stringResource(id = org.fossify.commons.R.string.general_settings))
            }) {
                if (isUseEnglishEnabled) {
                    SettingsSwitchComponent(
                        label = stringResource(id = org.fossify.commons.R.string.use_english_language),
                        initialValue = isUseEnglishChecked,
                        onChange = onUseEnglishPress,
                        showCheckmark = isShowingCheckmarksOnSwitches,
                    )
                }
                if (isTiramisuPlus()) {
                    SettingsPreferenceComponent(
                        label = stringResource(id = org.fossify.commons.R.string.language),
                        value = displayLanguage,
                        doOnPreferenceClick = onSetupLanguagePress,
                        preferenceLabelColor = SimpleTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        SettingsHorizontalDivider()
        SettingsGroup(title = {
            SettingsTitleTextComponent(text = stringResource(id = R.string.documents))
        }) {
            SettingsSwitchComponent(
                label = stringResource(id = R.string.show_file_locations),
                initialValue = showFileLocations,
                onChange = onShowFileLocationsChange,
                showCheckmark = isShowingCheckmarksOnSwitches,
            )
            SettingsSwitchComponent(
                label = stringResource(id = R.string.remember_pdf_page),
                initialValue = rememberPdfPage,
                onChange = onRememberPdfPageChange,
                showCheckmark = isShowingCheckmarksOnSwitches,
            )
        }
    }
}

@Composable
@MyDevices
private fun SettingsScreenPreview() {
    AppThemeSurface {
        SettingsScreen(
            displayLanguage = "English",
            isUseEnglishEnabled = false,
            isUseEnglishChecked = false,
            isShowingCheckmarksOnSwitches = false,
            rememberPdfPage = true,
            showFileLocations = false,
            onUseEnglishPress = {},
            onSetupLanguagePress = {},
            onRememberPdfPageChange = {},
            onShowFileLocationsChange = {},
            customizeColors = {},
            goBack = {},
        )
    }
}
