/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.groups

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.fixtures.aMessageEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemDebugInfo
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemRedactedContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemStateEventContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.virtual.aTimelineItemDaySeparatorModel
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.matrix.api.core.UniqueId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.core.FakeSendHandle
import io.element.android.libraries.matrix.ui.messages.reply.aProfileDetailsReady
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class TimelineItemGrouperTest {
    private val sut = TimelineItemGrouper()

    private val aGroupableItem = TimelineItem.Event(
        id = UniqueId("0"),
        senderId = A_USER_ID,
        senderAvatar = anAvatarData(),
        senderProfile = aProfileDetailsReady(displayName = ""),
        content = TimelineItemStateEventContent(body = "a state event"),
        reactionsState = aTimelineItemReactions(count = 0),
        readReceiptState = TimelineItemReadReceipts(emptyList<ReadReceiptData>().toImmutableList()),
        localSendState = LocalEventSendState.Sent(AN_EVENT_ID),
        isEditable = false,
        canBeRepliedTo = false,
        inReplyTo = null,
        threadInfo = null,
        origin = null,
        timelineItemDebugInfoProvider = { aTimelineItemDebugInfo() },
        messageShieldProvider = { null },
        sendHandleProvider = { FakeSendHandle() },
        forwarder = null,
        forwarderProfile = null,
    )
    private val aNonGroupableItem = aMessageEvent()
    private val aNonGroupableItemNoEvent = TimelineItem.Virtual(UniqueId("virtual"), aTimelineItemDaySeparatorModel("Today"))

    @Test
    fun `test empty`() {
        val result = sut.group(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `test non groupables`() {
        val result = sut.group(
            listOf(
                aNonGroupableItem,
                aNonGroupableItem,
            ),
        )
        assertThat(result).isEqualTo(
            listOf(
                aNonGroupableItem,
                aNonGroupableItem,
            )
        )
    }

    @Test
    fun `test groupables and ensure reordering`() {
        val result = sut.group(
            listOf(
                aGroupableItem.copy(id = UniqueId("1")),
                aGroupableItem.copy(id = UniqueId("0")),
            ),
        )
        assertThat(result).isEqualTo(
            listOf(
                TimelineItem.GroupedEvents(
                    id = computeGroupIdWith(aGroupableItem),
                    events = listOf(
                        aGroupableItem.copy(id = UniqueId("0")),
                        aGroupableItem.copy(id = UniqueId("1")),
                    ).toImmutableList(),
                    aggregatedReadReceipts = emptyList<ReadReceiptData>().toImmutableList(),
                ),
            )
        )
    }

    @Test
    fun `test 1 groupable, not group must be created`() {
        val listsToTest = listOf(
            listOf(aGroupableItem),
            listOf(aGroupableItem, aNonGroupableItem),
            listOf(aGroupableItem, aNonGroupableItemNoEvent),
            listOf(aNonGroupableItem, aGroupableItem),
            listOf(aNonGroupableItemNoEvent, aGroupableItem),
            listOf(aNonGroupableItem, aGroupableItem, aNonGroupableItem),
            listOf(aNonGroupableItemNoEvent, aGroupableItem, aNonGroupableItemNoEvent),
            listOf(aGroupableItem, aNonGroupableItem, aGroupableItem),
            listOf(aGroupableItem, aNonGroupableItemNoEvent, aGroupableItem),
            listOf(aNonGroupableItem),
            listOf(aNonGroupableItemNoEvent),
        )
        listsToTest.forEach { listToTest ->
            val result = sut.group(listToTest)
            assertThat(result).isEqualTo(listToTest)
        }
    }

    @Test
    fun `test 3 blocks`() {
        val result = sut.group(
            listOf(
                aGroupableItem,
                aGroupableItem,
                aNonGroupableItem,
                aGroupableItem,
                aGroupableItem,
                aGroupableItem,
            ),
        )
        assertThat(result).isEqualTo(
            listOf(
                TimelineItem.GroupedEvents(
                    id = computeGroupIdWith(aGroupableItem),
                    events = listOf(
                        aGroupableItem,
                        aGroupableItem,
                    ).toImmutableList(),
                    aggregatedReadReceipts = emptyList<ReadReceiptData>().toImmutableList(),
                ),
                aNonGroupableItem,
                TimelineItem.GroupedEvents(
                    id = computeGroupIdWith(aGroupableItem),
                    events = listOf(
                        aGroupableItem,
                        aGroupableItem,
                        aGroupableItem,
                    ).toImmutableList(),
                    aggregatedReadReceipts = emptyList<ReadReceiptData>().toImmutableList(),
                )
            )
        )
    }

    @Test
    fun `when calling multiple time the method group over a growing list of groupable items, then groupId is stable`() {
        // When
        val groupableItems = mutableListOf(
            aGroupableItem.copy(id = UniqueId("1")),
            aGroupableItem.copy(id = UniqueId("2"))
        )
        val expectedGroupId = sut.group(groupableItems).first().identifier()
        groupableItems.add(0, aGroupableItem.copy(UniqueId("3")))
        groupableItems.add(2, aGroupableItem.copy(UniqueId("4")))
        groupableItems.add(aGroupableItem.copy(UniqueId("5")))
        val actualGroupId = sut.group(groupableItems).first().identifier()
        // Then
        assertThat(actualGroupId).isEqualTo(expectedGroupId)
    }

    @Test
    fun `a run of three or more redacted events is collapsed into a single group as a final step`() {
        val r0 = aGroupableItem.copy(id = UniqueId("r0"), content = TimelineItemRedactedContent)
        val r1 = aGroupableItem.copy(id = UniqueId("r1"), content = TimelineItemRedactedContent)
        val r2 = aGroupableItem.copy(id = UniqueId("r2"), content = TimelineItemRedactedContent)
        val result = sut.group(listOf(r0, r1, r2))
        assertThat(result).isEqualTo(
            listOf(
                TimelineItem.GroupedEvents(
                    id = computeGroupIdWith(r2),
                    // Stored oldest-first (the input is newest-first) with the id keyed by the
                    // oldest member through the shared registry, like the other groups.
                    events = listOf(r2, r1, r0).toImmutableList(),
                    aggregatedReadReceipts = emptyList<ReadReceiptData>().toImmutableList(),
                )
            )
        )
    }

    @Test
    fun `a run of fewer than three redacted events is left untouched`() {
        val r0 = aGroupableItem.copy(id = UniqueId("r0"), content = TimelineItemRedactedContent)
        val r1 = aGroupableItem.copy(id = UniqueId("r1"), content = TimelineItemRedactedContent)
        val result = sut.group(listOf(r0, r1))
        assertThat(result).isEqualTo(listOf(r0, r1))
    }

    @Test
    fun `test image grid grouping and ensure reordering`() {
        val newestImage = anImageEvent(id = "newest", sentTimeMillis = 2_000)
        val oldestImage = anImageEvent(id = "oldest", sentTimeMillis = 0)

        val result = sut.group(listOf(newestImage, oldestImage))

        assertThat(result).isEqualTo(
            listOf(
                TimelineItem.ImageGrid(
                    id = computeGroupIdWith(oldestImage),
                    events = listOf(oldestImage, newestImage).toImmutableList(),
                ),
            )
        )
    }

    @Test
    fun `test image grid grouping requires same sender inside time window`() {
        val senderAImage1 = anImageEvent(id = "senderA-1", sentTimeMillis = 10_000)
        val senderAImage2 = anImageEvent(id = "senderA-2", sentTimeMillis = 6_000)
        val senderBImage = anImageEvent(id = "senderB", sentTimeMillis = 4_000, senderId = UserId("@other:domain"))
        val oldSenderAImage = anImageEvent(id = "senderA-old", sentTimeMillis = -2_000)

        val result = sut.group(listOf(senderAImage1, senderAImage2, senderBImage, oldSenderAImage))

        assertThat(result).isEqualTo(
            listOf(
                TimelineItem.ImageGrid(
                    id = computeGroupIdWith(senderAImage2),
                    events = listOf(senderAImage2, senderAImage1).toImmutableList(),
                ),
                senderBImage,
                oldSenderAImage,
            )
        )
    }

    @Test
    fun `test image with caption is not grouped into image grid`() {
        val captionedImage = anImageEvent(
            id = "captioned",
            sentTimeMillis = 2_000,
            content = aTimelineItemImageContent(caption = "Caption")
        )
        val plainImage = anImageEvent(id = "plain", sentTimeMillis = 0)

        val result = sut.group(listOf(captionedImage, plainImage))

        assertThat(result).isEqualTo(listOf(captionedImage, plainImage))
    }

    private fun anImageEvent(
        id: String,
        sentTimeMillis: Long,
        senderId: UserId = UserId("@senderId:domain"),
        content: TimelineItemImageContent = aTimelineItemImageContent(),
    ): TimelineItem.Event {
        return aTimelineItemEvent(
            content = content,
            timelineItemReactions = aTimelineItemReactions(count = 0),
        ).copy(
            id = UniqueId(id),
            senderId = senderId,
            sentTimeMillis = sentTimeMillis,
        )
    }
}
