// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Switch
import com.tailscale.ipn.R
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.mdm.ShowHide
import com.tailscale.ipn.ui.notifier.Notifier
import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.viewModel.ExitNodePickerNav
import com.tailscale.ipn.ui.viewModel.ExitNodePickerViewModel
import com.tailscale.ipn.ui.viewModel.selected
import kotlinx.coroutines.flow.MutableStateFlow

/** Android TV version of [ExitNodePicker]. */
@Composable
fun TvExitNodePicker(nav: ExitNodePickerNav, model: ExitNodePickerViewModel) {
  val tailnetExitNodes by model.tailnetExitNodes.collectAsState()
  val mullvadExitNodesByCountryCode by model.mullvadExitNodesByCountryCode.collectAsState()
  val mullvadExitNodeCount by model.mullvadExitNodeCount.collectAsState()
  val anyActive by model.anyActive.collectAsState()
  val shouldShowMullvadInfo by model.shouldShowMullvadInfo.collectAsState()
  val isRunningExitNode by model.isRunningExitNode.collectAsState()
  val allowLANAccess = Notifier.prefs.collectAsState().value?.ExitNodeAllowLANAccess == true
  val showRunAsExitNode by MDMSettings.runExitNode.flow.collectAsState()
  val allowLanAccessMDMDisposition by MDMSettings.exitNodeAllowLANAccess.flow.collectAsState()
  val managedByOrganization by model.managedByOrganization.collectAsState()
  val forcedExitNodeId = MDMSettings.exitNodeID.flow.collectAsState().value.value

  val noneLabel = stringResource(R.string.none)
  val none =
      ExitNodePickerViewModel.ExitNode(
          label = noneLabel,
          online = MutableStateFlow(true),
          selected = !anyActive,
      )
  val choices = if (forcedExitNodeId == null) listOf(none) + tailnetExitNodes else tailnetExitNodes
  val current =
      tailnetExitNodes.firstOrNull { it.selected }?.let { it.city.ifEmpty { it.label } }
          ?: if (mullvadExitNodesByCountryCode.selected) stringResource(R.string.mullvad_exit_nodes)
          else noneLabel

  val selectedFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    withFrameNanos {}
    runCatching { selectedFocus.requestFocus() }
  }

  LoadingIndicator.Wrap {
    TvScreen(
        title = stringResource(R.string.choose_exit_node),
        subtitle =
            if (forcedExitNodeId != null) {
              managedByOrganization.value?.let {
                stringResource(R.string.exit_node_mdm_orgname, it)
              } ?: stringResource(R.string.exit_node_mdm)
            } else current,
        side = {
          if (!allowLanAccessMDMDisposition.value.hiddenFromUser) {
            TvRow(
                headline = stringResource(R.string.allow_lan_access),
                trailing = { Switch(checked = allowLANAccess, onCheckedChange = null) },
            ) {
              LoadingIndicator.start()
              model.toggleAllowLANAccess { LoadingIndicator.stop() }
            }
          }
          if (showRunAsExitNode.value == ShowHide.Show) {
            TvRow(
                headline = stringResource(R.string.run_as_exit_node),
                supporting =
                    stringResource(if (isRunningExitNode) R.string.enabled else R.string.disabled),
                enabled = !anyActive,
                onClick = nav.onNavigateToRunAsExitNode,
            )
          }
          if (mullvadExitNodeCount > 0) {
            TvRow(
                headline = stringResource(R.string.mullvad_exit_nodes),
                supporting =
                    "${mullvadExitNodesByCountryCode.size} ${stringResource(R.string.countries)}",
                selected = mullvadExitNodesByCountryCode.selected,
                trailing = if (mullvadExitNodesByCountryCode.selected) ({ TvCheck() }) else null,
                onClick = nav.onNavigateToMullvad,
            )
          } else if (shouldShowMullvadInfo) {
            TvRow(
                headline = stringResource(R.string.mullvad_exit_nodes),
                supporting = stringResource(R.string.enable_in_the_admin_console),
                onClick = nav.onNavigateToMullvadInfo,
            )
          }
        },
    ) {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        items(choices, key = { it.id ?: "none" }) { node ->
          val online by node.online.collectAsState()
          TvRow(
              headline = node.city.ifEmpty { node.label },
              modifier = if (node.selected) Modifier.focusRequester(selectedFocus) else Modifier,
              supporting = if (!online) stringResource(R.string.offline) else null,
              selected = node.selected,
              enabled = online && !isRunningExitNode && forcedExitNodeId == null,
              leading = { TvStatusDot(online) },
              trailing = if (node.selected) ({ TvCheck() }) else null,
          ) {
            model.setExitNode(node)
          }
        }
      }
    }
  }
}
