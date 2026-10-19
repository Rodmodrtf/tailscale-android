// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.content.Context
import com.tailscale.ipn.ui.util.AndroidTVUtil

object TVSettings {
  private const val PREFS_NAME = "android_tv_settings"
  const val KEY_START_ON_BOOT = "start_on_boot"
  private const val KEY_USER_STOPPED = "user_stopped"

  /**
   * Whether Tailscale should be kept connected: started on boot, after an app update, on wake and
   * by the periodic keep-alive. Defaults to on for Android TV.
   */
  fun startOnBoot(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_START_ON_BOOT, AndroidTVUtil.isAndroidTV())
  }

  fun setStartOnBoot(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_START_ON_BOOT, enabled).apply()
    if (enabled) VpnKeepAlive.schedule(context) else VpnKeepAlive.cancel(context)
  }

  /**
   * Set when the user explicitly disconnects, so the keep-alive doesn't fight them. Cleared when
   * the VPN is started again and on boot.
   */
  fun userStopped(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_USER_STOPPED, false)
  }

  fun setUserStopped(context: Context, stopped: Boolean) {
    prefs(context).edit().putBoolean(KEY_USER_STOPPED, stopped).apply()
  }

  private fun prefs(context: Context) =
      context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
