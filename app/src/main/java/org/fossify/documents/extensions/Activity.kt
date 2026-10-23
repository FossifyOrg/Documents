package org.fossify.documents.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.TransactionTooLargeException
import android.provider.Settings
import androidx.annotation.RequiresApi
import org.fossify.commons.activities.AboutActivity
import org.fossify.commons.activities.CustomizationActivity
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.openDeviceSettings
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.APP_FAQ
import org.fossify.commons.helpers.APP_ICON_IDS
import org.fossify.commons.helpers.APP_LAUNCHER_NAME
import org.fossify.commons.helpers.APP_LICENSES
import org.fossify.commons.helpers.APP_NAME
import org.fossify.commons.helpers.APP_PACKAGE_NAME
import org.fossify.commons.helpers.APP_REPOSITORY_NAME
import org.fossify.commons.helpers.APP_VERSION_NAME
import org.fossify.commons.helpers.SHOW_FAQ_BEFORE_MAIL
import org.fossify.commons.models.FAQItem
import org.fossify.documents.R

@Suppress("TooGenericExceptionCaught")
internal fun Activity.shareDocuments(uris: List<Uri>, mimeType: String = "") {
    if (uris.isEmpty()) return

    try {
        val contentType = mimeType.ifBlank { getMimeTypeFromUri(uris.first()) }
            .ifBlank { "application/octet-stream" }
        val clips = ClipData.newUri(
            contentResolver,
            getString(org.fossify.commons.R.string.share),
            uris.first(),
        ).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = contentType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = clips
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        startActivity(Intent.createChooser(intent, getString(org.fossify.commons.R.string.share_via)))
    } catch (_: ActivityNotFoundException) {
        toast(org.fossify.commons.R.string.no_app_found)
    } catch (error: RuntimeException) {
        if (error.cause is TransactionTooLargeException) {
            toast(org.fossify.commons.R.string.maximum_share_reached)
        } else {
            showErrorToast(error)
        }
    }
}

internal fun Activity.startAboutActivity(
    appNameId: Int,
    licenseMask: Long,
    versionName: String,
    packageName: String,
    repositoryName: String,
    faqItems: ArrayList<FAQItem>,
    showFAQBeforeMail: Boolean,
    getAppIconIDs: ArrayList<Int> = getAppIconIDs(),
    getAppLauncherName: String = getString(R.string.app_launcher_name)
) {
    hideKeyboard()
    Intent(applicationContext, AboutActivity::class.java).apply {
        putExtra(APP_ICON_IDS, getAppIconIDs)
        putExtra(APP_LAUNCHER_NAME, getAppLauncherName)
        putExtra(APP_NAME, getString(appNameId))
        putExtra(APP_LICENSES, licenseMask)
        putExtra(APP_VERSION_NAME, versionName)
        putExtra(APP_PACKAGE_NAME, packageName)
        putExtra(APP_REPOSITORY_NAME, repositoryName)
        putExtra(APP_FAQ, faqItems)
        putExtra(SHOW_FAQ_BEFORE_MAIL, showFAQBeforeMail)
        startActivity(this)
    }
}

internal fun Activity.startCustomizationActivity(
    getAppIconIDs: ArrayList<Int> = getAppIconIDs(),
    getAppLauncherName: String = getString(R.string.app_launcher_name)
) {
    Intent(applicationContext, CustomizationActivity::class.java).apply {
        putExtra(APP_ICON_IDS, getAppIconIDs)
        putExtra(APP_LAUNCHER_NAME, getAppLauncherName)
        startActivity(this)
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun Activity.launchChangeAppLanguageIntent() {
    try {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    } catch (_: ActivityNotFoundException) {
        openDeviceSettings()
    } catch (_: SecurityException) {
        openDeviceSettings()
    }
}

private fun getAppIconIDs() = arrayListOf(
    R.mipmap.ic_launcher_red,
    R.mipmap.ic_launcher_pink,
    R.mipmap.ic_launcher_purple,
    R.mipmap.ic_launcher_deep_purple,
    R.mipmap.ic_launcher_indigo,
    R.mipmap.ic_launcher_blue,
    R.mipmap.ic_launcher_light_blue,
    R.mipmap.ic_launcher_cyan,
    R.mipmap.ic_launcher_teal,
    R.mipmap.ic_launcher,
    R.mipmap.ic_launcher_light_green,
    R.mipmap.ic_launcher_lime,
    R.mipmap.ic_launcher_yellow,
    R.mipmap.ic_launcher_amber,
    R.mipmap.ic_launcher_orange,
    R.mipmap.ic_launcher_deep_orange,
    R.mipmap.ic_launcher_brown,
    R.mipmap.ic_launcher_blue_grey,
    R.mipmap.ic_launcher_grey_black
)
