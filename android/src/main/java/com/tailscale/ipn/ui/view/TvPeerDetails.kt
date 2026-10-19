// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.theme.AppTheme
import com.tailscale.ipn.ui.viewModel.PeerDetailsViewModel

/** Android TV version of [PeerDetails]. */
@Composable
fun TvPeerDetails(model: PeerDetailsViewModel) {
  val isPinging by model.isPinging.collectAsState()
  val favorites by model.favorites.collectAsState()
  val isWriting by model.isWritingFavorites.collectAsState()
  val netmap = model.netmap.collectAsState().value ?: return
  val node = model.node.collectAsState().value ?: return
  val isPinned = favorites?.isFavoriteDevice(node.StableID) == true

  // Back closes the ping results before leaving the screen.
  BackHandler(enabled = isPinging) { model.onPingDismissal() }

  val pingFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { runCatching { pingFocus.requestFocus() } }

  TvScreen(
      title = node.displayName,
      subtitle = stringResource(node.connectedStrRes(netmap)),
      side = {
        TvRow(
            headline = stringResource(R.string.ping),
            modifier = Modifier.focusRequester(pingFocus),
            selected = isPinging,
            leading = { Icon(painterResource(R.drawable.sensors_24), contentDescription = null) },
        ) {
          if (isPinging) model.onPingDismissal() else model.startPing()
        }
        TvRow(
            headline = stringResource(if (isPinned) R.string.unpin_device else R.string.pin_device),
            enabled = !isWriting,
            leading = {
              Icon(
                  painterResource(if (isPinned) R.drawable.unpin_24 else R.drawable.pin_24),
                  contentDescription = null,
              )
            },
        ) {
          model.togglePin()
        }
      },
  ) {
    if (isPinging) {
      // PingView is a Material 3 phone view; give it the app theme it expects.
      AppTheme { PingView(model = model.pingViewModel) }
    } else {
      Text(
          text = stringResource(R.string.tailscale_addresses),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        node.displayAddresses.forEach { InfoPair(title = it.typeString, value = it.address) }
      }
      Spacer(Modifier.height(20.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        node.info.forEach {
          InfoPair(title = stringResource(it.titleRes), value = it.value.getString())
        }
      }
    }
  }
}

/** Read-only label and value; TV has no clipboard, so these aren't focusable. */
@Composable
private fun InfoPair(title: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
