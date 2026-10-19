// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.tailscale.ipn.AlwaysOnVpn
import com.tailscale.ipn.IPNService
import com.tailscale.ipn.R
import com.tailscale.ipn.TVSettings
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.ui.model.Ipn
import com.tailscale.ipn.ui.model.Tailcfg
import com.tailscale.ipn.ui.viewModel.IpnViewModel.NodeState
import com.tailscale.ipn.ui.viewModel.MainViewModel
import java.net.Inet4Address

private const val FOCUS_PRIMARY = "primary"

private val Green = Color(0xFF1CA672)
private val Gray = Color(0xFF706E6D)
private val Red = Color(0xFFEF5350)

private val TvColors =
    darkColorScheme(
        primary = Color(0xFF5A82DC),
        background = Color(0xFF232222),
        surface = Color(0xFF232222),
        surfaceVariant = Color(0xFF2E2D2D),
        onSurface = Color(0xFFFAF9F8),
        onSurfaceVariant = Color(0xFFAFACAB),
        inverseSurface = Color(0xFFFAF9F8),
        inverseOnSurface = Color(0xFF232222),
    )

/**
 * Android TV home screen built with Compose for TV (the replacement for Leanback). Sized for the
 * 960x540dp TV canvas inside the overscan margins applied by universalFit: controls on the left, a
 * D-pad navigable device grid on the right.
 */
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
  var alwaysConnected by remember { mutableStateOf(TVSettings.startOnBoot(context)) }
  var showAlwaysOnHelp by remember { mutableStateOf(false) }

  if (state == Ipn.State.Running) {
    viewModel.maybeRequestVpnPermission()
    LaunchVpnPermissionIfNeeded(viewModel)
    PromptForMissingPermissions(viewModel)
  }

  val self = netmap?.SelfNode
  val peers =
      netmap
          ?.Peers
          .orEmpty()
          .sortedWith(
              compareByDescending<Tailcfg.Node> { it.Online == true }.thenBy { it.displayName }
          )
  val selectedExitNodeId = prefs?.activeExitNodeID ?: prefs?.selectedExitNodeID
  val exitNodeName =
      selectedExitNodeId?.let { id -> peers.find { it.StableID == id }?.exitNodeName }
          ?: stringResource(R.string.none)
  val exitNodeOffline = nodeState == NodeState.OFFLINE_ENABLED || nodeState == NodeState.OFFLINE_MDM
  // IPNService.alwaysOn is what Android is applying now (API 29+); the setting is what applies
  // after the next restart.
  var alwaysOnConfigured by remember { mutableStateOf(AlwaysOnVpn.isConfigured(context)) }
  val alwaysOn = IPNService.alwaysOn ?: alwaysOnConfigured
  val connected = isOn && state == Ipn.State.Running

  // Coming back from a sub-screen, return focus to the item that opened it rather than the
  // Connect/Disconnect button, where a single OK press would disconnect.
  var lastFocusKey by rememberSaveable { mutableStateOf(FOCUS_PRIMARY) }
  val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
  val focusFor: (String) -> FocusRequester = { key ->
    focusRequesters.getOrPut(key) { FocusRequester() }
  }
  val opening: (String, () -> Unit) -> () -> Unit = { key, action ->
    {
      lastFocusKey = key
      action()
    }
  }
  LaunchedEffect(Unit) {
    withFrameNanos {} // let the device grid lay out first
    runCatching { focusFor(lastFocusKey).requestFocus() }
        .onFailure { runCatching { focusFor(FOCUS_PRIMARY).requestFocus() } }
  }

  MaterialTheme(colorScheme = TvColors) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(containerColor = TvColors.background),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        // Left: this TV, connection and controls.
        Column(modifier = Modifier.width(320.dp).fillMaxHeight()) {
          Text(
              text =
                  user?.NetworkProfile?.tailnetNameForDisplay()
                      ?: stringResource(R.string.app_name),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Text(
              text = self?.displayName ?: stringResource(R.string.tv_dashboard_this_device),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(connected, size = 10)
            Spacer(Modifier.width(8.dp))
            Text(
                text = listOfNotNull(stateStr, self?.primaryIPv4Address).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
          }
          Spacer(Modifier.height(14.dp))

          val buttonModifier = Modifier.focusRequester(focusFor(FOCUS_PRIMARY))
          val toggleEnabled = !disableToggle.value && !isToggleInProgress
          when {
            state == Ipn.State.NeedsLogin ->
                Button(onClick = { viewModel.login() }, modifier = buttonModifier) {
                  Text(stringResource(R.string.log_in))
                }
            state == Ipn.State.NeedsMachineAuth && self != null ->
                Button(onClick = { loginAtUrl(self.nodeAdminUrl) }, modifier = buttonModifier) {
                  Text(stringResource(R.string.open_admin_console))
                }
            else ->
                Button(
                    onClick = { viewModel.toggleVpn(!isOn) },
                    modifier = buttonModifier,
                    enabled = toggleEnabled,
                ) {
                  Text(
                      stringResource(
                          when {
                            isOn -> R.string.disconnect
                            isPrepared -> R.string.connect
                            else -> R.string._continue
                          }
                      )
                  )
                }
          }
          Spacer(Modifier.height(10.dp))

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingItem(
                label = stringResource(R.string.tv_always_connected),
                trailing = {
                  Switch(checked = alwaysConnected, onCheckedChange = null)
                },
            ) {
              alwaysConnected = !alwaysConnected
              TVSettings.setStartOnBoot(context, alwaysConnected)
            }
            SettingItem(
                label = stringResource(R.string.tv_always_on_vpn),
                value =
                    when {
                      alwaysOnConfigured == true && alwaysOn != true ->
                          stringResource(R.string.tv_always_on_after_restart)
                      alwaysOn == true -> stringResource(R.string.on)
                      alwaysOn == false && AlwaysOnVpn.canConfigure(context) ->
                          stringResource(R.string.off)
                      else -> stringResource(R.string.tv_always_on_unknown)
                    },
                valueColor = if (alwaysOn == true) Green else null,
            ) {
              when {
                AlwaysOnVpn.canConfigure(context) -> {
                  val enable = alwaysOnConfigured != true
                  if (AlwaysOnVpn.setConfigured(context, enable)) {
                    alwaysOnConfigured = AlwaysOnVpn.isConfigured(context)
                  }
                  showAlwaysOnHelp = false
                }
                openVpnSettings(context) -> {}
                else -> showAlwaysOnHelp = !showAlwaysOnHelp
              }
            }
            SettingItem(
                label =
                    stringResource(R.string.exit_node).lowercase().replaceFirstChar {
                      it.uppercase()
                    },
                value = exitNodeName,
                valueColor = if (exitNodeOffline) Red else null,
                modifier = Modifier.focusRequester(focusFor("exit-node")),
                onClick = opening("exit-node", navigation.onNavigateToExitNodes),
            )
            SettingItem(
                label = stringResource(R.string.tv_dashboard_health),
                value =
                    stringResource(
                        if (healthIcon != null) R.string.tv_dashboard_health_needs_attention
                        else R.string.tv_dashboard_health_ok
                    ),
                valueColor = if (healthIcon != null) Red else Green,
                modifier = Modifier.focusRequester(focusFor("health")),
                onClick = opening("health", navigation.onNavigateToHealth),
            )
            SettingItem(
                label = stringResource(R.string.tv_settings_tile),
                modifier = Modifier.focusRequester(focusFor("settings")),
                onClick = opening("settings", navigation.onNavigateToSettings),
            )
          }
        }

        // Right: devices on the tailnet (or Always-on VPN instructions).
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          if (showAlwaysOnHelp) {
            AlwaysOnHelp(context)
          } else {
            DevicesPane(
                state,
                peers,
                self != null,
                connected,
                selectedExitNodeId,
                navigation,
                focusFor,
                opening,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DevicesPane(
    state: Ipn.State,
    peers: List<Tailcfg.Node>,
    hasSelf: Boolean,
    selfOnline: Boolean,
    selectedExitNodeId: String?,
    navigation: MainViewNavigation,
    focusFor: (String) -> FocusRequester,
    opening: (String, () -> Unit) -> () -> Unit,
) {
  // Count this TV too, as the admin console does.
  val selfCount = if (hasSelf) 1 else 0
  val onlineCount = peers.count { it.Online == true } + if (hasSelf && selfOnline) 1 else 0
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = stringResource(R.string.tv_dashboard_devices),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )
      if (peers.isNotEmpty()) {
        Text(
            text =
                stringResource(
                    R.string.tv_dashboard_device_count,
                    onlineCount,
                    peers.size + selfCount,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (peers.isNotEmpty()) {
      Button(
          onClick = opening("search", navigation.onNavigateToSearch),
          modifier = Modifier.focusRequester(focusFor("search")),
      ) {
        Text(stringResource(R.string.search))
      }
    }
  }
  Spacer(Modifier.height(12.dp))

  when {
    state == Ipn.State.NeedsLogin -> MutedText(stringResource(R.string.tv_dashboard_sign_in_hint))
    state == Ipn.State.NeedsMachineAuth ->
        MutedText(stringResource(R.string.machine_auth_explainer))
    peers.isEmpty() -> MutedText(stringResource(R.string.tv_dashboard_no_devices))
    else ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            // Room for the focus scale so edge cards aren't clipped.
            contentPadding = PaddingValues(8.dp),
        ) {
          items(peers, key = { it.StableID }) { peer ->
            DeviceCard(
                peer = peer,
                isExitNode = peer.StableID == selectedExitNodeId,
                modifier = Modifier.focusRequester(focusFor("peer:${peer.StableID}")),
                onClick =
                    opening("peer:${peer.StableID}") { navigation.onNavigateToPeerDetails(peer) },
            )
          }
        }
  }
}

@Composable
private fun SettingItem(
    label: String,
    value: String? = null,
    valueColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
  ListItem(
      selected = false,
      onClick = onClick,
      modifier = modifier.height(48.dp),
      headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      trailingContent =
          trailing
              ?: {
                Text(
                    text = value ?: "›",
                    style = MaterialTheme.typography.labelLarge,
                    // Unset color lets the focused (inverted) item recolor the text.
                    color = valueColor ?: Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
              },
  )
}

@Composable
private fun DeviceCard(
    peer: Tailcfg.Node,
    isExitNode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
  val online = peer.Online == true
  Card(
      onClick = onClick,
      modifier = modifier.fillMaxWidth().height(72.dp),
      shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
      scale = CardDefaults.scale(focusedScale = 1.05f),
      colors =
          CardDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              focusedContainerColor = Color(0xFF444342),
          ),
      border =
          CardDefaults.border(
              focusedBorder =
                  Border(BorderStroke(2.dp, Color.White), shape = RoundedCornerShape(12.dp))
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusDot(online, size = 10)
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = peer.displayName,
            style = MaterialTheme.typography.titleSmall,
            color =
                if (online) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text =
                if (isExitNode) stringResource(R.string.tv_dashboard_exit_node_badge)
                else peer.primaryIPv4Address ?: peer.Hostinfo.OS ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isExitNode) Green else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun AlwaysOnHelp(context: Context) {
  Text(
      text = stringResource(R.string.tv_always_on_title),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
  )
  Spacer(Modifier.height(12.dp))
  Text(
      text = stringResource(R.string.tv_always_on_body, lanAddress(context) ?: "TV_IP"),
      style = MaterialTheme.typography.bodyLarge,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun StatusDot(on: Boolean, size: Int) {
  Box(modifier = Modifier.size(size.dp).background(if (on) Green else Gray, CircleShape))
}

@Composable
private fun MutedText(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

/** Opens Android's VPN settings. Many TVs (e.g. Mi Box) don't have this screen. */
private fun openVpnSettings(context: Context): Boolean {
  return try {
    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
    true
  } catch (e: ActivityNotFoundException) {
    false
  }
}

/** The TV's Wi-Fi/Ethernet IPv4 address, for the ADB instructions. */
private fun lanAddress(context: Context): String? {
  val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
  @Suppress("DEPRECATION")
  return cm.allNetworks
      .filter { network ->
        cm.getNetworkCapabilities(network)?.let {
          it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
              it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } == true
      }
      .flatMap { cm.getLinkProperties(it)?.linkAddresses.orEmpty() }
      .map { it.address }
      .firstOrNull { it is Inet4Address }
      ?.hostAddress
}
