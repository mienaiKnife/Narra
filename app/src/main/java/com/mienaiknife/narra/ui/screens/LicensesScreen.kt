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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
        Library("Coil", "Coil Contributors", "Apache 2.0"),
        Library("Dagger Hilt", "Google", "Apache 2.0"),
        Library("DataStore", "Google", "Apache 2.0"),
        Library("Epublib", "Paul Siegmann / Position Development", "LGPL-3.0"),
        Library("Jetpack Compose", "Google", "Apache 2.0"),
        Library("Jetpack Glance", "Google", "Apache 2.0"),
        Library("Jsoup", "Jonathan Hedley", "MIT"),
        Library("Kotlin", "JetBrains", "Apache 2.0"),
        Library("Kotlin Coroutines", "JetBrains", "Apache 2.0"),
        Library("Media3", "Google", "Apache 2.0"),
        Library("Navigation Compose", "Google", "Apache 2.0"),
        Library("OkHttp", "Square", "Apache 2.0"),
        Library("ONNX Runtime", "Microsoft", "MIT"),
        Library("Readability4J", "dankito", "Apache 2.0"),
        Library("Room Persistence Library", "Google", "Apache 2.0"),
        Library("RSS Parser", "Marco Gomiero", "Apache 2.0"),
        Library("Sherpa-ONNX", "k2-fsa", "Apache 2.0"),
        Library("SQLCipher for Android", "Zetetic LLC", "BSD-3-Clause"),
        Library("WorkManager", "Google", "Apache 2.0"),
        Library("Chime sounds (400 Sounds Pack)", "Chequered Ink", "Free for commercial use"),
    ).sortedBy { it.name }

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
                ListItem(
                    headlineContent = { Text(library.name) },
                    supportingContent = { Text("${library.author}${stringResource(R.string.separator_bullet)}${library.license}") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

data class Library(val name: String, val author: String, val license: String)

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
