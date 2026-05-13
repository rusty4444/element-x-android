/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.scheduledsend

import android.content.Context
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class ScheduledMessageInfo(
    val workId: String,
    val sessionId: String,
    val roomId: String,
    val body: String,
    val htmlBody: String?,
    val scheduledTimeMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    fun isPast(): Boolean = scheduledTimeMillis <= System.currentTimeMillis()

    fun formattedPreview(maxLength: Int = 120): String =
        if (body.length > maxLength) body.take(maxLength) + "…" else body
}

interface ScheduledSendManager {
    fun save(info: ScheduledMessageInfo)
    fun getAll(): List<ScheduledMessageInfo>
    fun getForRoom(sessionId: SessionId, roomId: RoomId): List<ScheduledMessageInfo>
    fun remove(workId: String)
    fun clearAll()
    fun observeRoom(sessionId: SessionId, roomId: RoomId): Flow<List<ScheduledMessageInfo>>
}

class DefaultScheduledSendManager(
    @ApplicationContext private val context: Context,
) : ScheduledSendManager {
    private val prefs by lazy {
        context.getSharedPreferences("scheduled_send_v1", Context.MODE_PRIVATE)
    }
    private val json = Json { ignoreUnknownKeys = true }

    private val dataFlow = MutableStateFlow(loadAll())

    private fun loadAll(): List<ScheduledMessageInfo> {
        val all = prefs.all
        return all.entries.asSequence()
            .filter { (key, _) -> key.startsWith("sched_") }
            .mapNotNull { (_, value) ->
                (value as? String)?.let { encoded ->
                    runCatching { json.decodeFromString<ScheduledMessageInfo>(encoded) }.getOrNull()
                }
            }
            .sortedBy { it.scheduledTimeMillis }
            .toList()
    }

    private fun keyFor(workId: String): String = "sched_$workId"

    override fun save(info: ScheduledMessageInfo) {
        val key = keyFor(info.workId)
        prefs.edit().putString(key, json.encodeToString(info)).apply()
        dataFlow.value = loadAll()
    }

    override fun getAll(): List<ScheduledMessageInfo> = dataFlow.value.toList()

    override fun getForRoom(sessionId: SessionId, roomId: RoomId): List<ScheduledMessageInfo> =
        dataFlow.value.filter { it.sessionId == sessionId.value && it.roomId == roomId.value }

    override fun remove(workId: String) {
        prefs.edit().remove(keyFor(workId)).apply()
        dataFlow.value = loadAll()
    }

    override fun clearAll() {
        val keys = prefs.all.keys.filter { it.startsWith("sched_") }
        prefs.edit().apply {
            keys.forEach { remove(it) }
        }.apply()
        dataFlow.value = emptyList()
    }

    override fun observeRoom(sessionId: SessionId, roomId: RoomId): Flow<List<ScheduledMessageInfo>> {
        return dataFlow.also { _ -> }
            .let { flow ->
                kotlinx.coroutines.flow.Flow { emit(flow.value) }
            }
    }
}
