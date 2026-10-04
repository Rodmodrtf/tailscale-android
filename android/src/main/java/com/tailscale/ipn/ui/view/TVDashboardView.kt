// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tailscale.ipn.R
import com.tailscale.ipn.TVSettings
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.ui.model.Ipn
import com.tailscale.ipn.ui.model.Tailcfg
import com.tailscale.ipn.ui.theme.off
import com.tailscale.ipn.ui.theme.on
import com.tailscale.ipn.ui.theme.success
import com.tailscale.ipn.ui.viewModel.IpnViewModel.NodeState
import com.tailscale.ipn.ui.viewModel.MainViewModel

@Composable
fun TVDashboardView(
    loginAtUrl: (String) -> Unit,
    navigation: MainViewNavigation,
    viewModel: MainViewModel,
) {
  val context = LocalContext.current
  val isPrepared by viewModel.isVpnPrepared.collectAsState()
  val isOn by viewModel.vpnToggleState.collectAsState()
  val state by viewModel.ipnState.collectAsState()
  val user by viewModel.loggedInUser.collectAsState()
  val stateVal by viewModel.stateRes.collectAsState(initial = R.string.placeholder)
  val stateStr = stringResource(id = stateVal)
  val netmap by viewModel.netmap.collectAsState()
  val prefs by viewModel.prefs.collectAsState()
  val nodeState by viewModel.nodeState.collectAsState()
  val disableToggle by MDMSettings.forceEnabled.flow.collectAsState()
  val isToggleInProgress by viewModel.isToggleInProgress.collectAsState()
  val healthIcon by viewModel.healthIcon.collectAsState()
  var startOnBoot by remember { mutableStateOf(TVSettings.startOnBoot(context)) }

  LaunchedTvVpnPermission(state, viewModel)

  val peers = netmap?.Peers.orEmpty()
  val onlinePeers = peers.count { it.Online == true }
  val exitNodes = peers.filter { it.isExitNode }
  val selectedExitNodeId = prefs?.activeExitNodeID ?: prefs?.selectedExitNodeID
  val exitNodeName =
      selectedExitNodeId?.let { id -> peers.find { it.StableID == id }?.exitNodeName }
          ?: stringResource(R.string.none)

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.surface)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 40.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TVStatusPanel(
          modifier = Modifier.weight(1.2f),
          tailnet = user?.NetworkProfile?.tailnetNameForDisplay() ?: stringResource(R.string.app_name),
          state = state,
          stateText = stateStr,
          isOn = isOn,
          enabled = !disableToggle.value && !isToggleInProgress,
          onToggle = { desired -> viewModel.toggleVpn(desired) },
          loginAction = { viewModel.login() },
          connectAction = { viewModel.toggleVpn(true) },
          isPrepared = isPrepared,
      )
      TVQuickStatsPanel(
          modifier = Modifier.weight(1f),
          totalPeers = peers.size,
          onlinePeers = onlinePeers,
          exitNodeName = exitNodeName,
          healthWarning = healthIcon != null,
          onHealth = navigation.onNavigateToHealth,
          onExitNodes = navigation.onNavigateToExitNodes,
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
      TVSection(title = stringResource(R.string.tv_dashboard_devices), modifier = Modifier.weight(1.35f)) {
        val visiblePeers =
            peers
                .sortedWith(
                    compareByDescending<Tailcfg.Node> { it.Online == true }.thenBy {
                      it.displayName
                    }
                )
                .take(8)
        if (visiblePeers.isEmpty()) {
          TVMutedText(stringResource(R.string.tv_dashboard_no_devices))
        } else {
          visiblePeers.forEach { peer ->
            TVPeerRow(peer = peer, onClick = { navigation.onNavigateToPeerDetails(peer) })
          }
        }
        Spacer(Modifier.height(8.dp))
        TVActionButton(text = stringResource(R.string.search), onClick = navigation.onNavigateToSearch)
      }

      TVSection(title = stringResource(R.string.tv_dashboard_exit_nodes), modifier = Modifier.weight(1f)) {
        if (exitNodes.isEmpty()) {
          TVMutedText(stringResource(R.string.tv_dashboard_no_exit_nodes))
        } else {
          exitNodes.take(5).forEach { node ->
            TVExitNodeRow(node = node, isSelected = node.StableID == selectedExitNodeId)
          }
        }
        Spacer(Modifier.height(8.dp))
        TVActionButton(text = stringResource(R.string.choose_exit_node), onClick = navigation.onNavigateToExitNodes)
      }
    }

    TVSection(title = stringResource(R.string.tv_dashboard_home_controls)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        TVToggleTile(
            title = stringResource(R.string.tv_start_on_boot),
            subtitle = stringResource(R.string.tv_start_on_boot_subtitle),
            checked = startOnBoot,
            onCheckedChange = {
              startOnBoot = it
              TVSettings.setStartOnBoot(context, it)
            },
            modifier = Modifier.weight(1f),
        )
        TVActionTile(
            title = stringResource(R.string.tv_always_on_vpn),
            subtitle = stringResource(R.string.tv_always_on_vpn_subtitle),
            action = stringResource(R.string.go_to_settings),
            modifier = Modifier.weight(1f),
        ) {
          context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }
        TVActionTile(
            title = stringResource(R.string.tv_settings_tile),
            subtitle = stringResource(R.string.tv_settings_tile_subtitle),
            action = stringResource(R.string.settings_title),
            modifier = Modifier.weight(1f),
            onClick = navigation.onNavigateToSettings,
        )
      }
    }

    if (state == Ipn.State.NeedsMachineAuth) {
      netmap?.SelfNode?.let { self ->
        TVSection(title = stringResource(R.string.machine_auth_required)) {
          TVMutedText(stringResource(R.string.machine_auth_explainer))
          Spacer(Modifier.height(8.dp))
          TVActionButton(
              text = stringResource(R.string.open_admin_console),
              onClick = { loginAtUrl(self.nodeAdminUrl) },
          )
        }
      }
    }

    if (nodeState == NodeState.OFFLINE_ENABLED || nodeState == NodeState.OFFLINE_MDM) {
      TVWarningBanner(
          title = stringResource(R.string.exit_node_offline),
          message = stringResource(R.string.exit_node_offline_mdm),
      )
    }
  }
}

@Composable
private fun LaunchedTvVpnPermission(state: Ipn.State, viewModel: MainViewModel) {
  if (state == Ipn.State.Running) {
    viewModel.maybeRequestVpnPermission()
    LaunchVpnPermissionIfNeeded(viewModel)
    PromptForMissingPermissions(viewModel)
  }
}

@Composable
private fun TVStatusPanel(
    modifier: Modifier = Modifier,
    tailnet: String,
    state: Ipn.State,
    stateText: String,
    isOn: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    loginAction: () -> Unit,
    connectAction: () -> Unit,
    isPrepared: Boolean,
) {
  TVPanel(modifier = modifier) {
    Text(
        text = tailnet,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Box(
          modifier =
              Modifier.size(14.dp)
                  .background(
                      if (isOn) MaterialTheme.colorScheme.on else MaterialTheme.colorScheme.off,
                      RoundedCornerShape(percent = 50),
                  )
      )
      Text(text = stateText, style = MaterialTheme.typography.titleLarge)
    }
    Spacer(Modifier.height(20.dp))
    when {
      state == Ipn.State.NeedsLogin -> TVPrimaryButton(text = stringResource(R.string.log_in), onClick = loginAction)
      state == Ipn.State.NeedsMachineAuth -> TVMutedText(stringResource(R.string.machine_auth_required))
      isOn -> {
        TVPrimaryButton(text = stringResource(R.string.disconnect), enabled = enabled) {
          onToggle(false)
        }
      }
      else -> {
        TVPrimaryButton(
            text =
                if (isPrepared) stringResource(R.string.connect)
                else stringResource(R.string._continue),
            enabled = enabled,
            onClick = connectAction,
        )
      }
    }
  }
}

@Composable
private fun TVQuickStatsPanel(
    modifier: Modifier = Modifier,
    totalPeers: Int,
    onlinePeers: Int,
    exitNodeName: String,
    healthWarning: Boolean,
    onHealth: () -> Unit,
    onExitNodes: () -> Unit,
) {
  TVPanel(modifier = modifier) {
    Text(
        text = stringResource(R.string.tv_dashboard_overview),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(14.dp))
    TVStatRow(stringResource(R.string.tv_dashboard_online_devices), "$onlinePeers / $totalPeers")
    TVStatRow(stringResource(R.string.exit_node), exitNodeName, onClick = onExitNodes)
    TVStatRow(
        stringResource(R.string.warning),
        if (healthWarning) stringResource(R.string.tv_dashboard_health_needs_attention)
        else stringResource(R.string.ok),
        onClick = if (healthWarning) onHealth else null,
    )
  }
}

@Composable
private fun TVSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
  TVPanel(modifier = modifier) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(14.dp))
    content()
  }
}

@Composable
private fun TVPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
      modifier =
          modifier
              .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      content = content,
  )
}

@Composable
private fun TVPeerRow(peer: Tailcfg.Node, onClick: () -> Unit) {
  TVFocusableRow(onClick = onClick) {
    Box(
        modifier =
            Modifier.size(12.dp)
                .background(
                    if (peer.Online == true) MaterialTheme.colorScheme.on
                    else MaterialTheme.colorScheme.off,
                    RoundedCornerShape(percent = 50),
                )
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = peer.displayName,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          text = peer.primaryIPv4Address ?: peer.magicDNSAddress ?: stringResource(R.string.placeholder),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun TVExitNodeRow(node: Tailcfg.Node, isSelected: Boolean) {
  TVFocusableRow {
    Box(
        modifier =
            Modifier.size(12.dp)
                .background(
                    if (node.Online == true) MaterialTheme.colorScheme.on
                    else MaterialTheme.colorScheme.off,
                    RoundedCornerShape(percent = 50),
                )
    )
    Text(
        text = node.exitNodeName,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (isSelected) {
      Text(
          text = stringResource(R.string.selected),
          color = MaterialTheme.colorScheme.success,
          style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun TVFocusableRow(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
  var focused by remember { mutableStateOf(false) }
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
              .onFocusChanged { focused = it.isFocused }
              .focusable()
              .background(
                  if (focused) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh,
                  RoundedCornerShape(6.dp),
              )
              .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      content = content,
  )
}

@Composable
private fun TVToggleTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
  TVPanel(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TVMutedText(subtitle)
      }
      Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
  }
}

@Composable
private fun TVActionTile(
    title: String,
    subtitle: String,
    action: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
  TVPanel(modifier = modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    TVMutedText(subtitle)
    Spacer(Modifier.height(8.dp))
    TVActionButton(text = action, onClick = onClick)
  }
}

@Composable
private fun TVStatRow(label: String, value: String, onClick: (() -> Unit)? = null) {
  TVFocusableRow(onClick = onClick) {
    Text(
        text = label,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun TVPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      enabled = enabled,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier = Modifier.height(54.dp),
  ) {
    Text(text, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun TVActionButton(text: String, onClick: () -> Unit) {
  OutlinedButton(onClick = onClick, modifier = Modifier.height(48.dp)) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun TVMutedText(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun TVWarningBanner(title: String, message: String) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
              .padding(16.dp)
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(4.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
  }
}
