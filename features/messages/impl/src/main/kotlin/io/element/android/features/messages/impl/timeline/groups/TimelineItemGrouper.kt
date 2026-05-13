/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.groups

import androidx.annotation.VisibleForTesting
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.matrix.api.core.UniqueId
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.abs

private const val IMAGE_GRID_MAX_TIME_WINDOW_MILLIS = 5_000L

@SingleIn(RoomScope::class)
@Inject
class TimelineItemGrouper {
    /**
     * Keys are identifier of items in a group, only one by group will be kept.
     * Values are the actual groupIds.
     * Cleared on each group() call to avoid stale groupId assignments when
     * items change reactable status (e.g., get a reaction or lose one).
     */
    private val groupIds = HashMap<String, String>()

    /**
     * Create a new list of [TimelineItem] by grouping some of them into [TimelineItem.GroupedEvents].
     * The internal groupId cache is cleared each time to ensure stale groupIds
     * from previous runs don't cause items to stick in/out of groups unexpectedly.
     */
    fun group(from: List<TimelineItem>): List<TimelineItem> {
        // Clear stale groupIds from previous runs — items may have gained/lost
        // reactions, replies, etc. which changes their groupability.
        groupIds.clear()

        val result = mutableListOf<TimelineItem>()
        val currentGroup = mutableListOf<TimelineItem.Event>()
        val currentImageGrid = mutableListOf<TimelineItem.Event>()

        fun flushCurrentGroup() {
            if (currentGroup.isNotEmpty()) {
                result.addGroup(groupIds, currentGroup)
                currentGroup.clear()
            }
        }

        fun flushCurrentImageGrid() {
            if (currentImageGrid.isNotEmpty()) {
                result.addImageGrid(groupIds, currentImageGrid)
                currentImageGrid.clear()
            }
        }

        from.forEach { timelineItem ->
            when {
                timelineItem is TimelineItem.Event && timelineItem.canBeGroupedAsImageGrid() -> {
                    flushCurrentGroup()
                    if (currentImageGrid.isNotEmpty() && !timelineItem.canBeGroupedWithImageGrid(currentImageGrid.first())) {
                        flushCurrentImageGrid()
                    }
                    currentImageGrid.add(0, timelineItem)
                }
                timelineItem is TimelineItem.Event && timelineItem.canBeGrouped() -> {
                    flushCurrentImageGrid()
                    currentGroup.add(0, timelineItem)
                }
                else -> {
                    flushCurrentGroup()
                    flushCurrentImageGrid()
                    result.add(timelineItem)
                }
            }
        }
        flushCurrentGroup()
        flushCurrentImageGrid()
        return result
    }
}

/**
 * Will add a group if there is more than 1 item, else add the item to the list.
 */
private fun MutableList<TimelineItem>.addGroup(
    groupIds: MutableMap<String, String>,
    groupOfItems: MutableList<TimelineItem.Event>
) {
    if (groupOfItems.size == 1) {
        // Do not create a group with just 1 item, just add the item to the result
        add(groupOfItems.first())
    } else {
        val groupId = groupIds.getOrPutGroupId(groupOfItems)
        add(
            TimelineItem.GroupedEvents(
                id = UniqueId(groupId),
                events = groupOfItems.toImmutableList(),
                aggregatedReadReceipts = groupOfItems.flatMap { it.readReceiptState.receipts }.toImmutableList()
            )
        )
    }
}

private fun MutableList<TimelineItem>.addImageGrid(
    groupIds: MutableMap<String, String>,
    groupOfItems: MutableList<TimelineItem.Event>
) {
    if (groupOfItems.size == 1) {
        add(groupOfItems.first())
    } else {
        val groupId = groupIds.getOrPutGroupId(groupOfItems)
        add(
            TimelineItem.ImageGrid(
                id = UniqueId(groupId),
                events = groupOfItems.toImmutableList(),
            )
        )
    }
}

private fun TimelineItem.Event.canBeGroupedWithImageGrid(other: TimelineItem.Event): Boolean {
    return senderId == other.senderId &&
        isMine == other.isMine &&
        abs(sentTimeMillis - other.sentTimeMillis) <= IMAGE_GRID_MAX_TIME_WINDOW_MILLIS
}

private fun MutableMap<String, String>.getOrPutGroupId(timelineItems: List<TimelineItem>): String {
    assert(timelineItems.isNotEmpty())
    for (item in timelineItems) {
        val itemIdentifier = item.identifier()
        if (this.contains(itemIdentifier.value)) {
            return this[itemIdentifier.value]!!
        }
    }
    val timelineItem = timelineItems.first()
    return computeGroupIdWith(timelineItem).value.also { groupId ->
        this[timelineItem.identifier().value] = groupId
    }
}

@VisibleForTesting
internal fun computeGroupIdWith(timelineItem: TimelineItem): UniqueId = UniqueId("${timelineItem.identifier()}_group")
