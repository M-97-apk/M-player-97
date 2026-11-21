package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object NativePermissionsHelper {
  fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

  fun buildOverlaySettingsIntent(context: Context): Intent =
    Intent(
      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
      Uri.parse("package:${context.packageName}"),
    )

  fun buildAppDetailsSettingsIntent(context: Context): Intent =
    Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.parse("package:${context.packageName}"),
    )
}
