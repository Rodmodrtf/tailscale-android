// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Checkbox
import com.tailscale.ipn.App
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.ui.viewModel.SplitTunnelAppPickerViewModel

/** Android TV version of [SplitTunnelAppPickerView]: OK on an app row toggles it. */
@Composable
fun TvSplitTunnelAppPickerView(model: SplitTunnelAppPickerViewModel) {
  val installedApps by model.installedApps.collectAsState()
  val selectedPackageNames by model.selectedPackageNames.collectAsState()
  val allowSelected by model.allowSelected.collectAsState()
  val builtInDisallowedPackageNames: List<String> = App.get().builtInDisallowedPackageNames
  val mdmIncludedPackages by model.mdmIncludedPackages.collectAsState()
  val mdmExcludedPackages by model.mdmExcludedPackages.collectAsState()
  val showSwitchDialog by model.showSwitchDialog.collectAsState()
  val managedByMDM =
      mdmExcludedPackages.value?.isNotEmpty() == true ||
          mdmIncludedPackages.value?.isNotEmpty() == true
  // Selected apps first, ordered by the selection when the screen opened so rows don't jump
  // under the cursor while toggling.
  val initiallySelected = remember(installedApps.isEmpty()) { selectedPackageNames }
  val apps =
      remember(installedApps, initiallySelected) {
        installedApps.sortedByDescending { initiallySelected.contains(it.packageName) }
      }
  val iconSize = 36.dp
  val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }

  if (showSwitchDialog) {
    SwitchAlertDialog(
        allowSelected = allowSelected,
        onConfirm = {
          model.showSwitchDialog.set(false)
          model.performSelectionSwitch()
        },
        onDismiss = { model.showSwitchDialog.set(false) },
    )
  }

  TvScreen(
      title = stringResource(R.string.split_tunneling),
      subtitle =
          stringResource(
              when {
                mdmExcludedPackages.value?.isNotEmpty() == true ->
                    R.string.certain_apps_are_not_routed_via_tailscale
                mdmIncludedPackages.value?.isNotEmpty() == true ->
                    R.string.only_specific_apps_are_routed_via_tailscale
                allowSelected -> R.string.selected_apps_will_access_tailscale
                else ->
                    R.string.selected_apps_will_access_the_internet_directly_without_using_tailscale
              }
          ),
      side = {
        if (!managedByMDM) {
          TvRow(
              headline =
                  stringResource(
                      if (allowSelected) R.string.count_included_apps
                      else R.string.count_excluded_apps,
                      selectedPackageNames.count(),
                  ),
              supporting =
                  stringResource(
                      if (allowSelected) R.string.switch_to_select_to_exclude
                      else R.string.switch_to_select_to_include
                  ),
              onClick = { model.showSwitchDialog.set(true) },
          )
        }
      },
  ) {
    if (installedApps.isEmpty()) {
      TvMutedText(stringResource(R.string.tv_loading_apps))
    } else if (!managedByMDM) {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        items(apps, key = { it.packageName }) { app ->
          val icon =
              remember(app.packageName, iconSizePx) {
                model.installedAppsManager.packageManager
                    .getApplicationIcon(app.packageName)
                    .toBitmap(width = iconSizePx, height = iconSizePx)
                    .asImageBitmap()
              }
          val checked = selectedPackageNames.contains(app.packageName)
          TvRow(
              headline = app.name,
              supporting = app.packageName,
              enabled = !builtInDisallowedPackageNames.contains(app.packageName),
              leading = {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(iconSize))
              },
              trailing = { Checkbox(checked = checked, onCheckedChange = null) },
          ) {
            if (checked) model.deselect(packageName = app.packageName)
            else model.select(packageName = app.packageName)
          }
        }
      }
    }
  }
}
