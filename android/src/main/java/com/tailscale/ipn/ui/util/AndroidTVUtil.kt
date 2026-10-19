// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.util

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tailscale.ipn.UninitializedApp
import com.tailscale.ipn.ui.util.AndroidTVUtil.isAndroidTV

object AndroidTVUtil {
  private val FEATURE_FIRETV = "amazon.hardware.fire_tv"

  fun isAndroidTV(): Boolean {
    val pm = UninitializedApp.get().packageManager
    return (pm.hasSystemFeature(@Suppress("deprecation") PackageManager.FEATURE_TELEVISION) ||
        pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        pm.hasSystemFeature(FEATURE_FIRETV))
  }
}

// Keeps content inside the TV overscan-safe area (5% horizontal, ~4.5% vertical of 960x540dp)
// iff we're running on Android TV.
fun Modifier.universalFit(): Modifier {
  return when (isAndroidTV()) {
    true -> this.padding(horizontal = 48.dp, vertical = 24.dp)
    false -> this
  }
}
