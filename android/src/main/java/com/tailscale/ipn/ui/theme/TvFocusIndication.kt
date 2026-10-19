// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Indication for Android TV: the default ripple only tints a focused item faintly, which is hard to
 * see from the couch. This fills the focused item and outlines it in white, like Google TV, for
 * every clickable that uses the theme's default indication.
 */
object TvFocusIndication : IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
      TvFocusIndicationNode(interactionSource)

  override fun equals(other: Any?): Boolean = other === this

  override fun hashCode(): Int = javaClass.hashCode()
}

private class TvFocusIndicationNode(private val interactionSource: InteractionSource) :
    Modifier.Node(), DrawModifierNode {
  private var focused = false
  private var pressed = false

  override fun onAttach() {
    coroutineScope.launch {
      var focusCount = 0
      var pressCount = 0
      interactionSource.interactions.collect { interaction ->
        when (interaction) {
          is FocusInteraction.Focus -> focusCount++
          is FocusInteraction.Unfocus -> focusCount--
          is PressInteraction.Press -> pressCount++
          is PressInteraction.Release,
          is PressInteraction.Cancel -> pressCount--
        }
        val nowFocused = focusCount > 0
        val nowPressed = pressCount > 0
        if (nowFocused != focused || nowPressed != pressed) {
          focused = nowFocused
          pressed = nowPressed
          invalidateDraw()
        }
      }
    }
  }

  override fun ContentDrawScope.draw() {
    val radius = CornerRadius(8.dp.toPx())
    drawContent()
    // Drawn over the content: list items paint their own opaque background.
    if (focused || pressed) {
      drawRoundRect(Color.White.copy(alpha = if (pressed) 0.20f else 0.10f), cornerRadius = radius)
    }
    if (focused) {
      val stroke = 2.dp.toPx()
      drawRoundRect(
          Color.White,
          topLeft = Offset(stroke / 2, stroke / 2),
          size = Size(size.width - stroke, size.height - stroke),
          cornerRadius = radius,
          style = Stroke(stroke),
      )
    }
  }
}
