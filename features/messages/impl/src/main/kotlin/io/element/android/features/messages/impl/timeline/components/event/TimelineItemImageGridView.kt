/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2023-2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.protection.ProtectedView
import io.element.android.libraries.designsystem.components.blurhash.blurHashBackground
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.ui.utils.a11y.isTalkbackActive
import kotlinx.collections.immutable.ImmutableList

private val IMAGE_GRID_GAP = 2.dp
private val IMAGE_GRID_CORNER_RADIUS = 8.dp
private val IMAGE_GRID_HEIGHT_TWO_IMAGES = 160.dp
private val IMAGE_GRID_HEIGHT_MULTI_IMAGES = 220.dp
private const val IMAGE_GRID_VISIBLE_ITEM_COUNT = 4

@Composable
fun TimelineItemImageGridView(
    events: ImmutableList<TimelineItem.Event>,
    hideMediaContent: (TimelineItem.Event) -> Boolean,
    onShowContentClick: (TimelineItem.Event) -> Unit,
    onContentClick: (TimelineItem.Event) -> Unit,
    onLongClick: (TimelineItem.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleEvents = events.take(IMAGE_GRID_VISIBLE_ITEM_COUNT)
    val hiddenCount = events.size - visibleEvents.size
    val height = if (events.size == 2) IMAGE_GRID_HEIGHT_TWO_IMAGES else IMAGE_GRID_HEIGHT_MULTI_IMAGES

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(IMAGE_GRID_CORNER_RADIUS)),
        verticalArrangement = Arrangement.spacedBy(IMAGE_GRID_GAP),
    ) {
        when (visibleEvents.size) {
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(IMAGE_GRID_GAP),
                ) {
                    visibleEvents.forEachIndexed { index, event ->
                        ImageGridTile(
                            event = event,
                            hiddenCount = if (index == visibleEvents.lastIndex) hiddenCount else 0,
                            hideMediaContent = hideMediaContent(event),
                            onShowContentClick = { onShowContentClick(event) },
                            onContentClick = { onContentClick(event) },
                            onLongClick = { onLongClick(event) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            3 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(IMAGE_GRID_GAP),
                ) {
                    ImageGridTile(
                        event = visibleEvents[0],
                        hiddenCount = 0,
                        hideMediaContent = hideMediaContent(visibleEvents[0]),
                        onShowContentClick = { onShowContentClick(visibleEvents[0]) },
                        onContentClick = { onContentClick(visibleEvents[0]) },
                        onLongClick = { onLongClick(visibleEvents[0]) },
                        modifier = Modifier.weight(1f),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(IMAGE_GRID_GAP),
                    ) {
                        visibleEvents.drop(1).forEachIndexed { index, event ->
                            ImageGridTile(
                                event = event,
                                hiddenCount = if (index == 1) hiddenCount else 0,
                                hideMediaContent = hideMediaContent(event),
                                onShowContentClick = { onShowContentClick(event) },
                                onContentClick = { onContentClick(event) },
                                onLongClick = { onLongClick(event) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            else -> {
                visibleEvents.chunked(2).forEachIndexed { rowIndex, rowEvents ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(IMAGE_GRID_GAP),
                    ) {
                        rowEvents.forEachIndexed { columnIndex, event ->
                            val isLastVisibleTile = rowIndex * 2 + columnIndex == visibleEvents.lastIndex
                            ImageGridTile(
                                event = event,
                                hiddenCount = if (isLastVisibleTile) hiddenCount else 0,
                                hideMediaContent = hideMediaContent(event),
                                onShowContentClick = { onShowContentClick(event) },
                                onContentClick = { onContentClick(event) },
                                onLongClick = { onLongClick(event) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowEvents.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageGridTile(
    event: TimelineItem.Event,
    hiddenCount: Int,
    hideMediaContent: Boolean,
    onShowContentClick: () -> Unit,
    onContentClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageContent = event.content as? TimelineItemImageContent ?: return
    val imageLabel = stringResource(CommonStrings.common_image)
    Box(
        modifier = modifier
            .fillMaxSize()
            .blurHashBackground(imageContent.blurhash, alpha = 0.9f)
            .then(
                if (!isTalkbackActive()) {
                    Modifier
                        .combinedClickable(
                            onClick = onContentClick,
                            onLongClick = onLongClick,
                        )
                        .onKeyboardContextMenuAction(onLongClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        ProtectedView(
            hideContent = hideMediaContent,
            onShowClick = onShowContentClick,
        ) {
            var isLoaded by remember { mutableStateOf(false) }
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isLoaded) Modifier.background(Color.White) else Modifier),
                model = imageContent.thumbnailMediaRequestData,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                contentDescription = imageLabel,
                onState = { isLoaded = it is AsyncImagePainter.State.Success },
            )
        }
        if (hiddenCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$hiddenCount",
                    color = Color.White,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
