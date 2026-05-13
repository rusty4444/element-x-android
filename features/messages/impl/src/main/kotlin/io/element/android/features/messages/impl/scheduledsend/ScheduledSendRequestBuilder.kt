/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.scheduledsend

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.workmanager.api.WorkManagerRequestBuilder
import io.element.android.libraries.workmanager.api.WorkManagerRequestType
import io.element.android.libraries.workmanager.api.WorkManagerRequestWrapper
import io.element.android.libraries.workmanager.api.workManagerTag
import java.util.concurrent.TimeUnit

interface ScheduledSendRequestBuilder : WorkManagerRequestBuilder {
    fun interface Factory {
        fun create(
            sessionId: SessionId,
            roomId: RoomId,
            body: String,
            htmlBody: String?,
            scheduledTimeMillis: Long,
        ): ScheduledSendRequestBuilder
    }

    companion object {
        const val SESSION_ID = "session_id"
        const val ROOM_ID = "room_id"
        const val BODY = "body"
        const val HTML_BODY = "html_body"
    }
}

@AssistedInject
class DefaultScheduledSendRequestBuilder(
    @Assisted private val sessionId: SessionId,
    @Assisted private val roomId: RoomId,
    @Assisted private val body: String,
    @Assisted private val htmlBody: String?,
    @Assisted private val scheduledTimeMillis: Long,
) : ScheduledSendRequestBuilder {
    @AssistedFactory
    @ContributesBinding(AppScope::class)
    interface Factory : ScheduledSendRequestBuilder.Factory {
        override fun create(
            sessionId: SessionId,
            roomId: RoomId,
            body: String,
            htmlBody: String?,
            scheduledTimeMillis: Long,
        ): DefaultScheduledSendRequestBuilder
    }

    override suspend fun build(): Result<List<WorkManagerRequestWrapper>> {
        val initialDelayMillis = (scheduledTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val inputData = Data.Builder()
            .putString(ScheduledSendRequestBuilder.SESSION_ID, sessionId.value)
            .putString(ScheduledSendRequestBuilder.ROOM_ID, roomId.value)
            .putString(ScheduledSendRequestBuilder.BODY, body)
            .apply {
                htmlBody?.let { putString(ScheduledSendRequestBuilder.HTML_BODY, it) }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<ScheduledSendWorker>()
            .setInputData(inputData)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(workManagerTag(sessionId, WorkManagerRequestType.SCHEDULED_SEND))
            .addTag("scheduled_send:${roomId.value}")
            .build()

        return Result.success(listOf(WorkManagerRequestWrapper(request)))
    }
}
