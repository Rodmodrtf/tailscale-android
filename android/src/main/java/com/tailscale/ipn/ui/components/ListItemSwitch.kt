// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.components

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tailscale.ipn.ui.theme.listItem
import com.tailscale.ipn.ui.util.AndroidTVUtil.isAndroidTV
import com.tailscale.ipn.ui.view.TintedSwitch

@Composable
fun ListItemSwitch(
    titleRes: Int = 0,
    title: String? = null,
    subtitle: String? = null,
    isOn: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit = {},
) {
  val tv = isAndroidTV()
  ListItem(
      modifier =
          if (tv) Modifier.toggleable(value = isOn, enabled = enabled, onValueChange = onToggle)
          else Modifier,
      colors = MaterialTheme.colorScheme.listItem,
      headlineContent = {
        Text(
            title ?: stringResource(titleRes),
            style = MaterialTheme.typography.bodyMedium,
        )
      },
      supportingContent =
          subtitle?.let {
            {
              Text(
                  it,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
      trailingContent = {
        TintedSwitch(
            checked = isOn,
            onCheckedChange = if (tv) null else onToggle,
            enabled = enabled,
        )
      },
  )
}
