// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.tailscale.ipn.util.TSLog

/**
 * Sets Tailscale as Android's Always-on VPN from inside the app. Android TVs often have no VPN
 * settings screen, and normal apps can't change this setting, so it needs a one-time `adb shell pm
 * grant com.tailscale.ipn android.permission.WRITE_SECURE_SETTINGS`. Android reads the setting when
 * the device starts, so a change applies after the next restart.
 */
object AlwaysOnVpn {
  private const val TAG = "AlwaysOnVpn"
  // Settings.Secure.ALWAYS_ON_VPN_APP, which is hidden from the SDK.
  private const val KEY_ALWAYS_ON_VPN_APP = "always_on_vpn_app"

  fun canConfigure(context: Context): Boolean =
      context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
          PackageManager.PERMISSION_GRANTED

  /** Whether Tailscale is the configured Always-on VPN, or null if the setting can't be read. */
  fun isConfigured(context: Context): Boolean? =
      try {
        Settings.Secure.getString(context.contentResolver, KEY_ALWAYS_ON_VPN_APP) ==
            context.packageName
      } catch (e: Exception) {
        null
      }

  /** Returns false if the setting couldn't be written. */
  fun setConfigured(context: Context, enabled: Boolean): Boolean =
      try {
        Settings.Secure.putString(
            context.contentResolver,
            KEY_ALWAYS_ON_VPN_APP,
            if (enabled) context.packageName else null,
        )
      } catch (e: Exception) {
        TSLog.e(TAG, "Failed to set Always-on VPN: $e")
        false
      }
}
