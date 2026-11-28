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
package com.mienaiknife.narra.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mienaiknife.narra.R
import com.mienaiknife.narra.ui.theme.NarraTheme

/**
 * The app's standard two-line list row with an optional leading icon. Settings, About, and Licenses
 * all use it so their rows share the same typography, leading icon size, and padding.
 *
 * The icon's side spacing comes from the row: [ListItem] insets the leading slot by its own
 * `LeadingContentEndPadding`, so the 16 dp screen inset and the 16 dp icon-to-text gap are equal.
 * Do not add padding to the icon on top of that.
 *
 * For rows with richer content (progress, multiple supporting lines, inline buttons) use a
 * [ListItem] with [narraListItemColors] and the same text styles.
 */
@Composable
fun NarraListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Transparent,
) {
    ListItem(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            },
        ),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = supportingContent ?: subtitle?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = leadingContent ?: leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        trailingContent = trailingContent,
        colors = narraListItemColors(containerColor),
    )
}

@Composable
fun narraListItemColors(containerColor: Color = Color.Transparent) = ListItemDefaults.colors(
    containerColor = containerColor,
)

@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919,
)
@Composable
private fun NarraListItemPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            NarraListItem(
                title = stringResource(R.string.settings_ui_title),
                subtitle = stringResource(R.string.settings_ui_subtitle),
                leadingIcon = Icons.Default.Settings,
                onClick = {},
            )
        }
    }
}
