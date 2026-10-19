// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.tailscale.ipn.util.TSLog

/**
 * Android TV and some other devices have no system folder picker (ACTION_OPEN_DOCUMENT_TREE), and
 * launching it there crashes the app. Taildrop then falls back to the app's internal storage.
 */
object DirectoryPicker {
  fun isAvailable(context: Context): Boolean =
      Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).resolveActivity(context.packageManager) != null
}

/** Opens the folder picker; returns false instead of crashing if the device has none. */
fun ActivityResultLauncher<Uri?>.launchDirectoryPicker(): Boolean =
    try {
      launch(null)
      true
    } catch (e: ActivityNotFoundException) {
      TSLog.e("DirectoryPicker", "No folder picker on this device: $e")
      false
    }
