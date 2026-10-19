// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starts Tailscale after boot and after the app is updated (installing kills the VPN). */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action
    if (action !in ACTIONS) return
    if (!TVSettings.startOnBoot(context)) return

    // A manual disconnect is kept across app updates but not across reboots.
    if (action != Intent.ACTION_MY_PACKAGE_REPLACED) TVSettings.setUserStopped(context, false)
    VpnKeepAlive.schedule(context)
    // Give Android time to start an Always-on VPN itself before we try; starting it twice while
    // the device is busy booting gets IPNService killed.
    VpnKeepAlive.requestCheck(context, action ?: "boot", BOOT_DELAY_SECONDS)
  }

  companion object {
    private const val BOOT_DELAY_SECONDS = 60L
    private val ACTIONS =
        setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
  }
}
