/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.scheduledsend

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.workmanager.api.di.MetroWorkerFactory
import io.element.android.libraries.workmanager.api.di.WorkerKey
import timber.log.Timber

@AssistedInject
class ScheduledSendWorker(
    @Assisted params: WorkerParameters,
    @ApplicationContext private val context: Context,
    private val matrixClientProvider: MatrixClientProvider,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(ScheduledSendRequestBuilder.SESSION_ID)?.let(::SessionId)
            ?: return Result.failure().also { Timber.e("ScheduledSendWorker: missing session ID") }
        val roomId = inputData.getString(ScheduledSendRequestBuilder.ROOM_ID)?.let(::RoomId)
            ?: return Result.failure().also { Timber.e("ScheduledSendWorker: missing room ID") }
        val body = inputData.getString(ScheduledSendRequestBuilder.BODY)
            ?: return Result.failure().also { Timber.e("ScheduledSendWorker: missing message body") }
        val htmlBody = inputData.getString(ScheduledSendRequestBuilder.HTML_BODY)

        if (body.isBlank()) {
            Timber.e("ScheduledSendWorker: refusing to send blank scheduled message")
            return Result.failure()
        }

        val client = matrixClientProvider.getOrRestore(sessionId).getOrElse { error ->
            Timber.e(error, "ScheduledSendWorker: failed to restore Matrix client for $sessionId")
            return Result.retry()
        }
        val room = client.getJoinedRoom(roomId) ?: run {
            Timber.e("ScheduledSendWorker: room $roomId not found or not joined")
            return Result.failure()
        }

        return room.liveTimeline.sendMessage(
            body = body,
            htmlBody = htmlBody,
            intentionalMentions = emptyList<IntentionalMention>(),
        ).fold(
            onSuccess = {
                Timber.d("ScheduledSendWorker: sent scheduled message to $roomId")
                Result.success()
            },
            onFailure = { error ->
                Timber.e(error, "ScheduledSendWorker: failed to send scheduled message to $roomId")
                Result.retry()
            }
        )
    }

    @ContributesIntoMap(AppScope::class, binding = binding<MetroWorkerFactory.WorkerInstanceFactory<*>>())
    @WorkerKey(ScheduledSendWorker::class)
    @AssistedFactory
    interface Factory : MetroWorkerFactory.WorkerInstanceFactory<ScheduledSendWorker>
}
