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

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    if (!TVSettings.startOnBoot(context)) return

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
  }
}
