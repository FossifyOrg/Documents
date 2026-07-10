package org.fossify.documents.extensions

import android.content.Context
import org.fossify.documents.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)
