/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.scheduledsend

import android.content.Context
import android.util.Base64
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

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

    companion object {
        // Encoding format: workId|sessionId|roomId|scheduledTime|createdAt|htmlBody|body
        // body is last since it may contain | chars
        private const val SEP = "|"

        fun encode(info: ScheduledMessageInfo): String {
            val body = Base64.encodeToString(info.body.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val html = info.htmlBody?.let { Base64.encodeToString(it.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) } ?: ""
            return listOf(
                info.workId,
                info.sessionId,
                info.roomId,
                info.scheduledTimeMillis.toString(),
                info.createdAtMillis.toString(),
                html,
                body
            ).joinToString(SEP)
        }

        fun decode(str: String): ScheduledMessageInfo? = try {
            val parts = str.split(SEP, limit = 7)
            if (parts.size < 7) return null
            val body = String(Base64.decode(parts[6], Base64.NO_WRAP), Charsets.UTF_8)
            val htmlRaw = parts[5]
            val htmlBody = if (htmlRaw.isEmpty()) {
                null
            } else {
                String(Base64.decode(htmlRaw, Base64.NO_WRAP), Charsets.UTF_8)
            }
            ScheduledMessageInfo(
                workId = parts[0],
                sessionId = parts[1],
                roomId = parts[2],
                body = body,
                htmlBody = htmlBody,
                scheduledTimeMillis = parts[3].toLong(),
                createdAtMillis = parts[4].toLong(),
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode scheduled message")
            null
        }
    }
}

interface ScheduledSendManager {
    fun save(info: ScheduledMessageInfo)
    fun getAll(): List<ScheduledMessageInfo>
    fun getForRoom(sessionId: SessionId, roomId: RoomId): List<ScheduledMessageInfo>
    fun observeForRoom(sessionId: SessionId, roomId: RoomId): Flow<List<ScheduledMessageInfo>>
    fun remove(workId: String)
    fun clearAll()
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DefaultScheduledSendManager(
    @ApplicationContext private val context: Context,
) : ScheduledSendManager {
    private val prefs by lazy {
        context.getSharedPreferences("scheduled_send_v1", Context.MODE_PRIVATE)
    }

    private val _allItems = MutableStateFlow<List<ScheduledMessageInfo>>(emptyList())

    init {
        _allItems.value = loadAll()
    }

    private fun loadAll(): List<ScheduledMessageInfo> = prefs.all.entries.asSequence()
        .filter { (key, _) -> key.startsWith("sched_") }
        .mapNotNull { (_, value) ->
            (value as? String)?.let { ScheduledMessageInfo.decode(it) }
        }
        .sortedBy { it.scheduledTimeMillis }
        .toList()

    private fun keyFor(workId: String): String = "sched_$workId"

    override fun save(info: ScheduledMessageInfo) {
        prefs.edit().putString(keyFor(info.workId), ScheduledMessageInfo.encode(info)).apply()
        _allItems.value = loadAll()
    }

    override fun getAll(): List<ScheduledMessageInfo> = loadAll()

    override fun getForRoom(sessionId: SessionId, roomId: RoomId): List<ScheduledMessageInfo> =
        _allItems.value.filter { it.sessionId == sessionId.value && it.roomId == roomId.value }

    override fun observeForRoom(sessionId: SessionId, roomId: RoomId): Flow<List<ScheduledMessageInfo>> =
        _allItems.map { list ->
            list.filter { it.sessionId == sessionId.value && it.roomId == roomId.value }
        }

    override fun remove(workId: String) {
        prefs.edit().remove(keyFor(workId)).apply()
        _allItems.value = loadAll()
    }

    override fun clearAll() {
        val keys = prefs.all.keys.filter { it.startsWith("sched_") }
        prefs.edit().apply { keys.forEach { remove(it) } }.apply()
        _allItems.value = loadAll()
    }
}
