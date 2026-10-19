// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.theme.exitNodeToggleButton
import com.tailscale.ipn.ui.util.DirectoryPicker
import com.tailscale.ipn.ui.util.Lists
import com.tailscale.ipn.ui.util.friendlyDirName
import com.tailscale.ipn.ui.util.launchDirectoryPicker
import com.tailscale.ipn.ui.viewModel.PermissionsViewModel
import com.tailscale.ipn.util.ShareFileHelper
import com.tailscale.ipn.util.TSLog

@Composable
fun TaildropDirView(
    backToPermissionsView: BackNavigation,
    openDirectoryLauncher: ActivityResultLauncher<Uri?>,
    permissionsViewModel: PermissionsViewModel,
) {
  Scaffold(
      topBar = { Header(titleRes = R.string.taildrop_dir_access, onBack = backToPermissionsView) }
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      item {
        ListItem(
            headlineContent = {
              Text(
                  stringResource(R.string.taildrop_dir_access),
                  style = MaterialTheme.typography.titleMedium,
              )
            },
            supportingContent = {
              Text(
                  text = stringResource(R.string.permission_taildrop_dir),
                  style = MaterialTheme.typography.bodyMedium,
              )
            },
        )
      }

      item("divider0") { Lists.SectionDivider() }

      item {
        val currentDir by permissionsViewModel.currentDir.collectAsState()
        TSLog.d("TaildropDirView", "currentDir in UI: $currentDir")
        val displayPath = currentDir?.let { friendlyDirName(it) } ?: "No access"

        ListItem(
            headlineContent = {
              Text(
                  text = stringResource(R.string.dir_access),
                  style = MaterialTheme.typography.titleMedium,
              )
            },
            supportingContent = {
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = displayPath, style = MaterialTheme.typography.bodyMedium)
                if (DirectoryPicker.isAvailable(LocalContext.current)) {
                  Button(
                      colors = MaterialTheme.colorScheme.exitNodeToggleButton,
                      onClick = { openDirectoryLauncher.launchDirectoryPicker() },
                      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                  ) {
                    Text(stringResource(R.string.pick_dir))
                  }
                } else {
                  Text(
                      text =
                          stringResource(
                              R.string.no_dir_picker,
                              ShareFileHelper.plainDirDisplayPath() ?: "Taildrop",
                          ),
                      style = MaterialTheme.typography.bodyMedium,
                      modifier = Modifier.padding(top = 12.dp),
                  )
                }
              }
            },
        )
      }
    }
  }
}
