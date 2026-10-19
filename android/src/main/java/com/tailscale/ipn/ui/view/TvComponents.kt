// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme

// Shared building blocks for the Android TV screens, built on Compose for TV (androidx.tv).
// Screens are laid out for the 960x540dp TV canvas inside universalFit's overscan margins.

internal val TvGreen = Color(0xFF1CA672)
internal val TvGray = Color(0xFF706E6D)
internal val TvRed = Color(0xFFEF5350)

internal val TvColors =
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

/** Compose for TV theme and background for a TV screen. */
@Composable
fun TvTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = TvColors) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(containerColor = TvColors.background),
        content = { content() },
    )
  }
}

/**
 * Two-pane TV screen like Android TV's own settings: title, description and secondary controls on
 * the left, the main list on the right. Back is the remote's Back button, so there's no back arrow.
 */
@Composable
fun TvScreen(
    title: String,
    subtitle: String? = null,
    side: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
  TvTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
      Column(modifier = Modifier.width(300.dp).fillMaxHeight()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
          Spacer(Modifier.height(6.dp))
          Text(
              text = it,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = side)
      }
      // End padding leaves room for focused rows, which grow slightly.
      Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp), content = content)
    }
  }
}

/** A focusable TV list row. */
@Composable
fun TvRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingColor: Color? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
  ListItem(
      selected = selected,
      enabled = enabled,
      onClick = onClick,
      modifier = modifier,
      headlineContent = { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      supportingContent =
          if (supporting != null) {
            {
              Text(
                  supporting,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  // Unset color lets the focused (inverted) row recolor the text.
                  color = supportingColor ?: Color.Unspecified,
              )
            }
          } else null,
      leadingContent = if (leading != null) ({ leading() }) else null,
      trailingContent = trailing,
  )
}

/** Small section heading inside a TV list. */
@Composable
fun TvSectionLabel(text: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
  )
}

@Composable
fun TvCheck() {
  Icon(Icons.Outlined.Check, contentDescription = null)
}

@Composable
fun TvStatusDot(on: Boolean, size: Int = 10) {
  Box(modifier = Modifier.size(size.dp).background(if (on) TvGreen else TvGray, CircleShape))
}

@Composable
fun TvMutedText(text: String, modifier: Modifier = Modifier) {
  Text(
      text = text,
      modifier = modifier,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
