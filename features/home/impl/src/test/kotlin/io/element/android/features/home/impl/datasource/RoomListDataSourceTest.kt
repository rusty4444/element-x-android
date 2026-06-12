/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.datasource

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.FakeDateTimeObserver
import io.element.android.libraries.androidutils.system.DateTimeObserver
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.notificationsettings.FakeNotificationSettingsService
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.matrix.test.room.aRemoteLatestEvent
import io.element.android.libraries.matrix.test.room.aRoomMember
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.roomlist.FakeDynamicRoomList
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import io.element.android.services.analytics.test.FakeAnalyticsService
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class RoomListDataSourceTest {
    @Test
    fun `when DateTimeObserver gets a date change, the room summaries are refreshed`() = runTest {
        val roomList = FakeDynamicRoomList().apply {
            summaries.emit(listOf(aRoomSummary()))
        }
        val roomListService = FakeRoomListService(
            createRoomListLambda = { roomList }
        ).apply {
            postState(RoomListService.State.Running)
        }
        val dateTimeObserver = FakeDateTimeObserver()
        var dateFormatterResult = "Today"
        val dateFormatter = FakeDateFormatter({ _, _, _ -> dateFormatterResult })
        val roomListDataSource = createRoomListDataSource(
            roomListService = roomListService,
            roomListRoomSummaryFactory = aRoomListRoomSummaryFactory(
                dateFormatter = dateFormatter,
            ),
            dateTimeObserver = dateTimeObserver,
        )

        roomListDataSource.roomSummariesFlow.test {
            // Observe room list items changes
            roomListDataSource.launchIn(backgroundScope)
            // Get the initial room list
            val initialRoomList = awaitItem()
            assertThat(initialRoomList).isNotEmpty()
            assertThat(initialRoomList.first().timestamp).isEqualTo("Today")
            dateFormatterResult = "Yesterday"
            // Trigger a date change
            dateTimeObserver.given(DateTimeObserver.Event.DateChanged(Instant.MIN, Instant.now()))
            // Check there is a new list and it's not the same as the previous one
            val newRoomList = awaitItem()
            assertThat(newRoomList).isNotSameInstanceAs(initialRoomList)
            assertThat(newRoomList.first().timestamp).isEqualTo("Yesterday")
        }
    }

    @Test
    fun `when DateTimeObserver gets a time zone change, the room summaries are refreshed`() = runTest {
        val roomList = FakeDynamicRoomList(summaries = MutableStateFlow(listOf(aRoomSummary())))
        val roomListService = FakeRoomListService(
            createRoomListLambda = { roomList }
        ).apply {
            postState(RoomListService.State.Running)
        }
        val dateTimeObserver = FakeDateTimeObserver()
        var dateFormatterResult = "Today"
        val dateFormatter = FakeDateFormatter({ _, _, _ -> dateFormatterResult })
        val roomListDataSource = createRoomListDataSource(
            roomListService = roomListService,
            roomListRoomSummaryFactory = aRoomListRoomSummaryFactory(
                dateFormatter = dateFormatter,
            ),
            dateTimeObserver = dateTimeObserver,
        )
        roomListDataSource.roomSummariesFlow.test {
            // Observe room list items changes
            roomListDataSource.launchIn(backgroundScope)
            // Get the initial room list
            val initialRoomList = awaitItem()
            assertThat(initialRoomList).isNotEmpty()
            assertThat(initialRoomList.first().timestamp).isEqualTo("Today")
            dateFormatterResult = "Yesterday"
            // Trigger a timezone change
            dateTimeObserver.given(DateTimeObserver.Event.TimeZoneChanged)
            // Check there is a new list and it's not the same as the previous one
            val newRoomList = awaitItem()
            assertThat(newRoomList).isNotSameInstanceAs(initialRoomList)
            assertThat(newRoomList.first().timestamp).isEqualTo("Yesterday")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `room heroes are cached per room even when latest event timestamp changes`() = runTest {
        var getMembersCallCount = 0
        val joinedRoom = FakeJoinedRoom(
            baseRoom = FakeBaseRoom(
                getMembersResult = {
                    getMembersCallCount++
                    Result.success(listOf(aRoomMember()))
                }
            )
        )
        val matrixClient = FakeMatrixClient().apply {
            givenGetRoomResult(A_ROOM_ID, joinedRoom)
        }
        val roomList = FakeDynamicRoomList().apply {
            summaries.emit(
                listOf(
                    aRoomSummary(
                        activeMembersCount = 2,
                        latestEvent = aRemoteLatestEvent(timestamp = 1L),
                    )
                )
            )
        }
        val roomListService = FakeRoomListService(createRoomListLambda = { roomList })
        val dateTimeObserver = FakeDateTimeObserver()
        val roomListDataSource = createRoomListDataSource(
            matrixClient = matrixClient,
            roomListService = roomListService,
            dateTimeObserver = dateTimeObserver,
        )

        roomListDataSource.roomSummariesFlow.test {
            roomListDataSource.launchIn(backgroundScope)
            awaitItem()
            assertThat(getMembersCallCount).isEqualTo(1)

            roomList.summaries.emit(
                listOf(
                    aRoomSummary(
                        activeMembersCount = 2,
                        latestEvent = aRemoteLatestEvent(timestamp = 2L),
                    )
                )
            )
            advanceTimeBy(101)
            awaitItem()

            dateTimeObserver.given(DateTimeObserver.Event.DateChanged(Instant.MIN, Instant.now()))
            awaitItem()

            assertThat(getMembersCallCount).isEqualTo(1)
        }
    }

    private fun TestScope.createRoomListDataSource(
        matrixClient: FakeMatrixClient = FakeMatrixClient(),
        roomListService: FakeRoomListService = FakeRoomListService(),
        roomListRoomSummaryFactory: RoomListRoomSummaryFactory = aRoomListRoomSummaryFactory(),
        notificationSettingsService: FakeNotificationSettingsService = FakeNotificationSettingsService(),
        dateTimeObserver: FakeDateTimeObserver = FakeDateTimeObserver(),
    ) = RoomListDataSource(
        matrixClient = matrixClient,
        roomListService = roomListService,
        roomListRoomSummaryFactory = roomListRoomSummaryFactory,
        coroutineDispatchers = testCoroutineDispatchers(),
        notificationSettingsService = notificationSettingsService,
        sessionCoroutineScope = backgroundScope,
        dateTimeObserver = dateTimeObserver,
        analyticsService = FakeAnalyticsService(),
    )
}
