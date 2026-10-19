// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
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
            ensureRunning(context, intent.action ?: "wake")
          }
        },
        filter,
    )

    try {
      val cm = app.getSystemService(ConnectivityManager::class.java)
      cm.registerDefaultNetworkCallback(
          object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
              ensureRunning(app, "network available")
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
   * Starts the VPN if keep-alive is enabled, the user didn't disconnect on purpose, the service
   * isn't already running and VPN permission is still granted (it is lost if another VPN app took
   * over, and we must not fight it).
   */
  fun ensureRunning(context: Context, reason: String) {
    if (!TVSettings.startOnBoot(context) || TVSettings.userStopped(context)) return
    if (IPNService.isActive) return
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
    VpnKeepAlive.ensureRunning(applicationContext, "periodic check")
    return Result.success()
  }
}
