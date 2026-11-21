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

import android.content.res.Configuration
import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.components.BottomNavBar
import com.mienaiknife.narra.ui.components.ScreenHeader
import com.mienaiknife.narra.ui.theme.NarraTheme

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val libraries = listOf(
        Library("Android Jetpack", "Google", "Apache 2.0"),
        Library("Apache Commons Compress", "Apache Software Foundation", "Apache 2.0"),
        Library("Chime sounds (400 Sounds Pack)", "Chequered Ink", "Free for commercial use"),
        Library("Coil", "Coil Contributors", "Apache 2.0"),
        Library("Dagger Hilt", "Google", "Apache 2.0"),
        Library("DataStore", "Google", "Apache 2.0"),
        Library("Epublib", "Paul Siegmann / Position Development", "LGPL-3.0", R.raw.license_epublib),
        Library("Jetpack Compose", "Google", "Apache 2.0"),
        Library("Jetpack Glance", "Google", "Apache 2.0"),
        Library("Jsoup", "Jonathan Hedley", "MIT"),
        Library("Kotlin", "JetBrains", "Apache 2.0"),
        Library("Kotlin Coroutines", "JetBrains", "Apache 2.0"),
        Library("Media3", "Google", "Apache 2.0"),
        Library("Navigation Compose", "Google", "Apache 2.0"),
        Library("OkHttp", "Square", "Apache 2.0"),
        Library("ONNX Runtime", "Microsoft", "MIT"),
        Library("OpenDyslexic 3", "Abbie Gonzalez", "SIL OFL 1.1", R.raw.license_opendyslexic),
        Library("Readability4J", "dankito", "Apache 2.0"),
        Library("Room Persistence Library", "Google", "Apache 2.0"),
        Library("RSS Parser", "Marco Gomiero", "Apache 2.0"),
        Library("Sherpa-ONNX", "k2-fsa", "Apache 2.0"),
        Library("SQLCipher for Android", "Zetetic LLC", "BSD-3-Clause"),
        Library("WorkManager", "Google", "Apache 2.0"),
    ).sortedBy { it.name }

    var selectedLibrary by remember { mutableStateOf<Library?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ScreenHeader(
            title = stringResource(R.string.reader_menu_licenses),
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(libraries, key = { it.name }) { library ->
                val openLabel = stringResource(R.string.licenses_view_full_text)
                ListItem(
                    modifier = if (library.licenseTextResId != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = openLabel) {
                            selectedLibrary = library
                        }
                    } else {
                        Modifier
                    },
                    headlineContent = { Text(library.name) },
                    supportingContent = { Text("${library.author}${stringResource(R.string.separator_bullet)}${library.license}") },
                    trailingContent = if (library.licenseTextResId != null) {
                        {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    selectedLibrary?.let { library ->
        val licenseTextResId = library.licenseTextResId
        if (licenseTextResId != null) {
            LicenseTextDialog(
                library = library,
                text = rememberLicenseText(licenseTextResId),
                onDismiss = { selectedLibrary = null },
            )
        }
    }
}

@Composable
private fun LicenseTextDialog(
    library: Library,
    text: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(library.name) },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun rememberLicenseText(@RawRes resId: Int): String {
    val resources = LocalResources.current
    return remember(resId) {
        resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }
}

data class Library(
    val name: String,
    val author: String,
    val license: String,
    @RawRes val licenseTextResId: Int? = null,
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun LicensesScreenPreview() {
    val navController = rememberNavController()
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                LicensesScreen(onBack = {})
            }
        }
    }
}
