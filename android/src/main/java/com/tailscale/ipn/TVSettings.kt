// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.content.Context

object TVSettings {
  private const val PREFS_NAME = "android_tv_settings"
  const val KEY_START_ON_BOOT = "start_on_boot"

  fun startOnBoot(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_START_ON_BOOT, false)
  }

  fun setStartOnBoot(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_START_ON_BOOT, enabled).apply()
  }

  private fun prefs(context: Context) =
      context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
