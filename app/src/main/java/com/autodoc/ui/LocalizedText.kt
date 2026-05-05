package com.autodoc.ui

import android.content.Context
import java.util.Locale

/**
 * Non-Compose fallback for background workers, validators and ViewModel messages.
 * Compose UI must use Android string resources via stringResource(R.string.*).
 */
fun localizedText(ro: String, en: String): String {
    return if (Locale.getDefault().language == "ro") ro else en
}

fun Context.localizedText(ro: String, en: String): String {
    val language = resources.configuration.locales[0].language
    return if (language == "ro") ro else en
}
