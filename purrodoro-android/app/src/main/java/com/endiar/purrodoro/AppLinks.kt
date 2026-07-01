package com.endiar.purrodoro

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLinks {
    const val PRIVACY_POLICY = "https://nandrasaputra.dev/purrodoro/privacy-policy"
    const val DELETE_ACCOUNT = "https://nandrasaputra.dev/purrodoro/delete-account"
    const val APP_PAGE = "https://nandrasaputra.dev/purrodoro"

    fun openExternal(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val launch = Intent.createChooser(intent, null)
        runCatching { context.startActivity(launch) }
    }
}
