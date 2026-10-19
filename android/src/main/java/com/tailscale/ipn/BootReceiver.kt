// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

/** Starts Tailscale after boot and after the app is updated (installing kills the VPN). */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action
    if (action !in ACTIONS) return
    if (!TVSettings.startOnBoot(context)) return

    if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      // Respect a manual disconnect across updates.
      VpnKeepAlive.ensureRunning(context, "app updated")
      return
    }

    TVSettings.setUserStopped(context, false)
    val request =
        OneTimeWorkRequest.Builder(StartVPNWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WORK_START_ON_BOOT)
            .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(WORK_START_ON_BOOT, ExistingWorkPolicy.REPLACE, request)
  }

  companion object {
    private const val WORK_START_ON_BOOT = "tv-start-vpn-on-boot"
    private val ACTIONS =
        setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
  }
}
