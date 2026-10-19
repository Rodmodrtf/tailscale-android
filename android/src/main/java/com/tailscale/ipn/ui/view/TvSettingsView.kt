// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Switch
import com.tailscale.ipn.BuildConfig
import com.tailscale.ipn.R
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.ui.util.AppVersion
import com.tailscale.ipn.ui.viewModel.SettingsNav

/** Android TV version of [SettingsView]; state and actions come from SettingsView. */
@Composable
fun TvSettingsView(
    settingsNav: SettingsNav,
    accountName: String?,
    tailnetName: String?,
    corpDNSEnabled: Boolean?,
    tailnetLockEnabled: Boolean?,
    showTailnetLock: Boolean,
    showSubnetRouting: Boolean,
    remoteLoggingEnabled: Boolean,
    onToggleRemoteLogging: () -> Unit,
    managedByOrganization: String?,
) {
  val focus = rememberTvFocusMemory("dns")

  TvScreen(
      title = stringResource(R.string.settings_title),
      subtitle = "${stringResource(R.string.version)} ${AppVersion.Short()}",
      side = {
        accountName?.let {
          TvRow(
              headline = it,
              supporting = tailnetName,
              modifier = focus.item("account"),
              onClick = focus.opening("account", settingsNav.onNavigateToUserSwitcher),
          )
        }
        managedByOrganization?.let {
          TvRow(
              headline = stringResource(R.string.managed_by_orgName, it),
              modifier = focus.item("managed"),
              onClick = focus.opening("managed", settingsNav.onNavigateToManagedBy),
          )
        }
        TvRow(
            headline = stringResource(R.string.about_tailscale),
            modifier = focus.item("about"),
            onClick = focus.opening("about", settingsNav.onNavigateToAbout),
        )
        TvRow(
            headline = stringResource(R.string.bug_report),
            modifier = focus.item("bug"),
            onClick = focus.opening("bug", settingsNav.onNavigateToBugReport),
        )
        if (BuildConfig.DEBUG) {
          TvRow(
              headline = stringResource(R.string.mdm_settings),
              supporting = stringResource(R.string.internal_debug_options),
              modifier = focus.item("mdm"),
              onClick = focus.opening("mdm", settingsNav.onNavigateToMDMSettings),
          )
        }
      },
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
      item {
        TvRow(
            headline = stringResource(R.string.dns_settings),
            supporting =
                corpDNSEnabled?.let {
                  stringResource(
                      if (it) R.string.using_tailscale_dns else R.string.not_using_tailscale_dns
                  )
                },
            modifier = focus.item("dns"),
            onClick = focus.opening("dns", settingsNav.onNavigateToDNSSettings),
        )
      }
      item {
        TvRow(
            headline = stringResource(R.string.split_tunneling),
            supporting = stringResource(R.string.filter_apps_allowed_to_access_tailscale),
            modifier = focus.item("split"),
            onClick = focus.opening("split", settingsNav.onNavigateToSplitTunneling),
        )
      }
      if (showTailnetLock) {
        item {
          TvRow(
              headline = stringResource(R.string.tailnet_lock),
              supporting =
                  tailnetLockEnabled?.let {
                    stringResource(if (it) R.string.enabled else R.string.disabled)
                  },
              modifier = focus.item("lock"),
              onClick = focus.opening("lock", settingsNav.onNavigateToTailnetLock),
          )
        }
      }
      if (showSubnetRouting) {
        item {
          TvRow(
              headline = stringResource(R.string.subnet_routing),
              modifier = focus.item("subnet"),
              onClick = focus.opening("subnet", settingsNav.onNavigateToSubnetRouting),
          )
        }
      }
      item {
        TvRow(
            headline = stringResource(R.string.client_remote_logging_enabled),
            supporting =
                stringResource(
                    if (MDMSettings.isMDMConfigured)
                        R.string.client_remote_logging_enabled_subtitle_mdm
                    else R.string.client_remote_logging_enabled_subtitle
                ),
            enabled = !MDMSettings.isMDMConfigured,
            trailing = { Switch(checked = remoteLoggingEnabled, onCheckedChange = null) },
            onClick = onToggleRemoteLogging,
        )
      }
    }
  }
}
