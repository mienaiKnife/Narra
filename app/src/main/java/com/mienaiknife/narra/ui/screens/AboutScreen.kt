/*
 * Copyright 2025 Narra Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mienaiknife.narra.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.NarraListItem
import com.mienaiknife.narra.ui.components.SettingsScaffold
import com.mienaiknife.narra.ui.theme.NarraTheme

@Composable
fun AboutScreen(onBack: () -> Unit, onNavigateToLicenses: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isPreview = LocalInspectionMode.current
    val unknownString = stringResource(R.string.reader_unknown)
    val previewString = stringResource(R.string.reader_preview)
    val versionName = if (isPreview) previewString else getVersionName(context, unknownString)

    AboutContent(
        versionName = versionName,
        onBack = onBack,
        onOpenRepository = { uriHandler.openUri("https://github.com/mienaiKnife/Narra") },
        onOpenPrivacyPolicy = { uriHandler.openUri("https://github.com/mienaiKnife/Narra/blob/main/docs/PRIVACY.md") },
        onNavigateToLicenses = onNavigateToLicenses,
    )
}

@Composable
fun AboutContent(
    versionName: String,
    onBack: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onNavigateToLicenses: () -> Unit,
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack,
        horizontalPadding = 0.dp,
    ) {
        NarraListItem(
            title = stringResource(R.string.about_version),
            subtitle = versionName,
            leadingIcon = Icons.Default.Info,
        )

        NarraListItem(
            title = stringResource(R.string.about_code_repository),
            subtitle = stringResource(R.string.about_code_repository_desc),
            leadingIcon = Icons.Default.Code,
            onClick = onOpenRepository,
        )

        NarraListItem(
            title = stringResource(R.string.reader_menu_licenses),
            subtitle = stringResource(R.string.about_licenses_desc),
            leadingIcon = Icons.Default.Description,
            onClick = onNavigateToLicenses,
        )

        NarraListItem(
            title = stringResource(R.string.about_privacy_policy),
            leadingIcon = Icons.Default.Info,
            onClick = onOpenPrivacyPolicy,
        )
    }
}

@PreviewLightDark
@Composable
fun AboutScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AboutContent(
                    versionName = "1.0.0",
                    onBack = {},
                    onOpenRepository = {},
                    onOpenPrivacyPolicy = {},
                    onNavigateToLicenses = {},
                )
            }
        }
    }
}

private fun getVersionName(context: Context, unknownString: String): String = try {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    packageInfo.versionName ?: unknownString
} catch (_: Exception) {
    unknownString
}
