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

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mienaiknife.narra.data.models.SortOption
import com.mienaiknife.narra.ui.theme.NarraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    onDismissRequest: () -> Unit,
    isQueue: Boolean = false,
    keepSorted: Boolean = false,
    onKeepSortedChange: (Boolean) -> Unit = {},
    showPlayed: Boolean = false,
    onShowPlayedChange: ((Boolean) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        SortBottomSheetContent(
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected,
            isQueue = isQueue,
            keepSorted = keepSorted,
            onKeepSortedChange = onKeepSortedChange,
            showPlayed = showPlayed,
            onShowPlayedChange = onShowPlayedChange
        )
    }
}

@Composable
fun SortBottomSheetContent(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    isQueue: Boolean = false,
    keepSorted: Boolean = false,
    onKeepSortedChange: (Boolean) -> Unit = {},
    showPlayed: Boolean = false,
    onShowPlayedChange: ((Boolean) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dateActive = selectedOption == SortOption.DATE_DESC || selectedOption == SortOption.DATE_ASC
            val titleActive = selectedOption == SortOption.TITLE_ASC || selectedOption == SortOption.TITLE_DESC
            val sourceActive = selectedOption == SortOption.SOURCE_ASC || selectedOption == SortOption.SOURCE_DESC

            SortButton(
                text = "Date",
                isActive = dateActive,
                onClick = {
                    onOptionSelected(SortOption.DATE_DESC)
                },
                icon = if (dateActive) {
                    if (selectedOption == SortOption.DATE_ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                } else null,
                modifier = Modifier.weight(1f)
            )
            SortButton(
                text = "Title",
                isActive = titleActive,
                onClick = {
                    onOptionSelected(SortOption.TITLE_ASC)
                },
                icon = if (titleActive) {
                    if (selectedOption == SortOption.TITLE_ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                } else null,
                modifier = Modifier.weight(1f)
            )
            SortButton(
                text = "Source",
                isActive = sourceActive,
                onClick = {
                    onOptionSelected(SortOption.SOURCE_ASC)
                },
                icon = if (sourceActive) {
                    if (selectedOption == SortOption.SOURCE_ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                } else null,
                modifier = Modifier.weight(1f)
            )
        }

        if (isQueue) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onKeepSortedChange(!keepSorted) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keep sorted",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = keepSorted,
                    onCheckedChange = onKeepSortedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }

        if (onShowPlayedChange != null) {
            Spacer(modifier = Modifier.height(if (isQueue) 0.dp else 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowPlayedChange(!showPlayed) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show played",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = showPlayed,
                    onCheckedChange = onShowPlayedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun SortButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val contentPadding = ButtonDefaults.TextButtonContentPadding
    if (isActive) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = contentPadding
        ) {
            SortButtonContent(text, icon)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = contentPadding
        ) {
            SortButtonContent(text, icon)
        }
    }
}

@Composable
private fun SortButtonContent(
    text: String,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
            modifier = if (icon != null) Modifier.weight(1f, fill = false) else Modifier
        )
        if (icon != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF191919
)
@Composable
private fun SortBottomSheetComparisonPreview() {
    NarraTheme(darkTheme = true, dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface {
                SortBottomSheetContent(
                    selectedOption = SortOption.DATE_DESC,
                    onOptionSelected = {},
                )
            }
        }
    }
}
