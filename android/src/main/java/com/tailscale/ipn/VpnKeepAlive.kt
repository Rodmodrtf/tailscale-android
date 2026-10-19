// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tailscale.ipn.ui.util.AndroidTVUtil
import com.tailscale.ipn.util.TSLog
import java.util.concurrent.TimeUnit

/**
 * Keeps Tailscale connected on devices (Android TV) where it should always be on. Android only
 * restarts a VPN by itself when it is set as the Always-on VPN, so this reconnects on boot, app
 * update, wake from standby, network changes and from a periodic worker.
 */
object VpnKeepAlive {
  private const val TAG = "VpnKeepAlive"
  private const val PERIODIC_WORK = "vpn-keep-alive"
  private const val DELAYED_CHECK_WORK = "vpn-keep-alive-check"
  private const val CHECK_DELAY_SECONDS = 30L

  /** Registers wake/network listeners for the lifetime of the process. Called from App.onCreate. */
  fun init(app: App) {
    if (!AndroidTVUtil.isAndroidTV()) return
    if (TVSettings.startOnBoot(app)) schedule(app)

    val filter =
        IntentFilter().apply {
          addAction(Intent.ACTION_SCREEN_ON)
          addAction(Intent.ACTION_USER_PRESENT)
          addAction(Intent.ACTION_DREAMING_STOPPED)
        }
    app.registerReceiver(
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            requestCheck(context, intent.action ?: "wake")
          }
        },
        filter,
    )

    try {
      val cm = app.getSystemService(ConnectivityManager::class.java)
      cm.registerDefaultNetworkCallback(
          object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
              requestCheck(app, "network available")
            }
          }
      )
    } catch (e: Exception) {
      TSLog.e(TAG, "registerDefaultNetworkCallback failed: $e")
    }
  }

  fun schedule(context: Context) {
    val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
  }

  /**
   * Checks the VPN after a delay instead of immediately. Events fire while the process is still
   * starting (often because Android itself is starting IPNService for Always-on VPN); starting it a
   * second time then makes Android kill the slow-starting service. Repeated events within the delay
   * collapse into one check.
   */
  fun requestCheck(context: Context, reason: String, delaySeconds: Long = CHECK_DELAY_SECONDS) {
    val request =
        OneTimeWorkRequestBuilder<KeepAliveWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(workDataOf(KeepAliveWorker.KEY_REASON to reason))
            .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(DELAYED_CHECK_WORK, ExistingWorkPolicy.KEEP, request)
  }

  /** Whether IPNService is running or starting, including when Android started it itself. */
  private fun serviceRunning(context: Context): Boolean {
    if (IPNService.isActive) return true
    val am = context.getSystemService(ActivityManager::class.java) ?: return false
    // getRunningServices is deprecated but still returns the caller's own services.
    @Suppress("DEPRECATION")
    return am.getRunningServices(Int.MAX_VALUE).any {
      it.service.packageName == context.packageName &&
          it.service.className == IPNService::class.java.name &&
          it.started
    }
  }

  /**
   * Starts the VPN if keep-alive is enabled, the user didn't disconnect on purpose, the service
   * isn't already running and VPN permission is still granted (it is lost if another VPN app took
   * over, and we must not fight it).
   */
  fun ensureRunning(context: Context, reason: String) {
    if (!TVSettings.startOnBoot(context) || TVSettings.userStopped(context)) return
    if (serviceRunning(context)) return
    val app = UninitializedApp.get()
    if (!app.isAbleToStartVPN()) return
    if (VpnService.prepare(context) != null) {
      TSLog.d(TAG, "Not reconnecting ($reason): VPN permission not granted")
      return
    }
    TSLog.d(TAG, "Reconnecting VPN: $reason")
    app.startVPN()
  }
}

class KeepAliveWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
  override fun doWork(): Result {
    VpnKeepAlive.ensureRunning(
        applicationContext,
        inputData.getString(KEY_REASON) ?: "periodic check",
    )
    return Result.success()
  }

  companion object {
    const val KEY_REASON = "reason"
  }
}
