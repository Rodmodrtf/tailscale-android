// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Switch
import com.tailscale.ipn.R
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.ui.notifier.Notifier
import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.viewModel.DNSEnablementState
import com.tailscale.ipn.ui.viewModel.DNSSettingsViewModel

/** Android TV version of [DNSSettingsView]. */
@Composable
fun TvDNSSettingsView(model: DNSSettingsViewModel) {
  val state: DNSEnablementState by model.enablementState.collectAsState()
  val dnsConfig = model.dnsConfig.collectAsState().value
  val resolvers = dnsConfig?.Resolvers.orEmpty().map { it.Addr.orEmpty() }
  val domains = dnsConfig?.Domains.orEmpty()
  val routes = dnsConfig?.Routes.orEmpty().mapNotNull { (name, rs) -> rs?.let { name to it } }
  val useCorpDNS = Notifier.prefs.collectAsState().value?.CorpDNS == true
  val dnsSettingsMDMDisposition by MDMSettings.useTailscaleDNSSettings.flow.collectAsState()
  val stateTint = state.tint()

  LoadingIndicator.Wrap {
    TvScreen(
        title = stringResource(R.string.dns_settings),
        subtitle = stringResource(state.caption),
        side = {
          TvRow(
              headline = stringResource(state.title),
              leading = {
                Icon(
                    painter = painterResource(state.symbolDrawable),
                    contentDescription = null,
                    tint = stateTint,
                    modifier = Modifier.size(28.dp),
                )
              },
              onClick = {},
          )
          if (!dnsSettingsMDMDisposition.value.hiddenFromUser) {
            TvRow(
                headline = stringResource(R.string.use_ts_dns),
                trailing = { Switch(checked = useCorpDNS, onCheckedChange = null) },
            ) {
              LoadingIndicator.start()
              model.toggleCorpDNS { LoadingIndicator.stop() }
            }
          }
        },
    ) {
      // Values are read-only (no clipboard on TV) but focusable so the list can scroll.
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        if (resolvers.isNotEmpty()) {
          item { TvSectionLabel(stringResource(R.string.resolvers)) }
          items(resolvers) { TvRow(headline = it, onClick = {}) }
        }
        if (domains.isNotEmpty()) {
          item { TvSectionLabel(stringResource(R.string.search_domains)) }
          items(domains) { TvRow(headline = it, onClick = {}) }
        }
        routes.forEach { (name, routeResolvers) ->
          item { TvSectionLabel("Route: $name") }
          items(routeResolvers) { TvRow(headline = it.Addr.orEmpty(), onClick = {}) }
        }
      }
    }
  }
}
