// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.Links
import com.tailscale.ipn.ui.theme.defaultTextColor
import com.tailscale.ipn.ui.theme.link
import com.tailscale.ipn.ui.util.AndroidTVUtil.isAndroidTV
import com.tailscale.ipn.ui.util.ClipboardValueView
import com.tailscale.ipn.ui.util.Lists
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.ui.viewModel.BugReportViewModel

@Composable
fun BugReportView(backToSettings: BackNavigation, model: BugReportViewModel = viewModel()) {
  val bugReportID by model.bugReportID.collectAsState()

  if (isAndroidTV()) {
    TvScreen(
        title = stringResource(R.string.bug_report_title),
        subtitle =
            stringResource(R.string.bug_report_instructions_prefix) +
                stringResource(R.string.bug_report_instructions_linktext) +
                stringResource(R.string.bug_report_instructions_suffix) +
                " (" +
                Links.SUPPORT_URL +
                ")",
    ) {
      // Shown large so it can be read off the screen; TV has no clipboard.
      androidx.tv.material3.Text(
          text = stringResource(R.string.bug_report_id),
          style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
          color = androidx.tv.material3.MaterialTheme.colorScheme.onSurfaceVariant,
      )
      androidx.tv.material3.Text(
          text = bugReportID,
          style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(vertical = 12.dp),
      )
      TvMutedText(stringResource(R.string.bug_report_id_desc))
    }
    return
  }

  Scaffold(topBar = { Header(R.string.bug_report_title, onBack = backToSettings) }) { innerPadding
    ->
    Column(
        modifier =
            Modifier.padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
    ) {
      Lists.MultilineDescription {
        Text(text = contactText(), style = MaterialTheme.typography.bodyMedium)
      }

      ClipboardValueView(bugReportID, title = stringResource(R.string.bug_report_id))

      Lists.InfoItem(stringResource(id = R.string.bug_report_id_desc))
    }
  }
}

@Composable
fun contactText(): AnnotatedString {
  val annotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.defaultTextColor)) {
      append(stringResource(id = R.string.bug_report_instructions_prefix))
    }

    pushLink(LinkAnnotation.Url(Links.SUPPORT_URL))
    withStyle(
        style =
            SpanStyle(
                color = MaterialTheme.colorScheme.link,
                textDecoration = TextDecoration.Underline,
            )
    ) {
      append(stringResource(id = R.string.bug_report_instructions_linktext))
    }
    pop()

    withStyle(SpanStyle(color = MaterialTheme.colorScheme.defaultTextColor)) {
      append(stringResource(id = R.string.bug_report_instructions_suffix))
    }
  }
  return annotatedString
}

@Preview
@Composable
fun BugReportPreview() {
  val vm = BugReportViewModel()
  vm.bugReportID.set("12345678ABCDEF-12345678ABCDEF")
  BugReportView({}, vm)
}
