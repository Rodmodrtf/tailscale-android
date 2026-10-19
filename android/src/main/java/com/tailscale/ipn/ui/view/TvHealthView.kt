// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.model.Health
import com.tailscale.ipn.ui.viewModel.HealthViewModel

private val TvAmber = Color(0xFFE5A23A)

/** Android TV version of [HealthView]. */
@Composable
fun TvHealthView(model: HealthViewModel) {
  val warnings by model.warnings.collectAsState()

  TvScreen(
      title = stringResource(R.string.health_warnings),
      subtitle =
          if (warnings.isEmpty()) stringResource(R.string.tailscale_is_operating_normally)
          else null,
  ) {
    if (warnings.isEmpty()) {
      Icon(
          painter = painterResource(R.drawable.check_circle),
          contentDescription = null,
          tint = TvGreen,
          modifier = Modifier.size(56.dp),
      )
      Spacer(Modifier.height(12.dp))
      Text(stringResource(R.string.no_issues_found), style = MaterialTheme.typography.titleLarge)
    } else {
      // Rows are focusable (with no action) so long lists can be scrolled with the D-pad.
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        items(warnings) { warning ->
          TvRow(
              headline = warning.Title.ifEmpty { warning.Text },
              supporting = if (warning.Title.isNotEmpty()) warning.Text else null,
              supportingMaxLines = 6,
              leading = { TvSeverityDot(warning.Severity) },
              onClick = {},
          )
        }
      }
    }
  }
}

@Composable
private fun TvSeverityDot(severity: Health.Severity) {
  Icon(
      painter = painterResource(R.drawable.warning),
      contentDescription = severity.name,
      tint =
          when (severity) {
            Health.Severity.high -> TvRed
            Health.Severity.medium -> TvAmber
            Health.Severity.low -> TvGray
          },
  )
}
