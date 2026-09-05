/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.model

import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class SelectRoomInfo(
    val roomId: RoomId,
    val name: String?,
    val canonicalAlias: RoomAlias?,
    val avatarUrl: String?,
    val heroes: ImmutableList<MatrixUser>,
    val heroAvatarData: ImmutableList<AvatarData> = persistentListOf(),
    val isDm: Boolean,
    val isTombstoned: Boolean,
) {
    fun getAvatarData(size: AvatarSize) = AvatarData(
        id = roomId.value,
        name = name,
        url = avatarUrl,
        size = size,
    )

    fun getHeroAvatarData(size: AvatarSize) = heroAvatarData.takeIf { it.isNotEmpty() }
        ?: heroes.withoutBridgeBotHeroes().map { user -> user.getAvatarData(size = size) }.toImmutableList()
}

fun RoomSummary.toSelectRoomInfo(heroAvatarData: ImmutableList<AvatarData> = persistentListOf()) = info.toSelectRoomInfo(heroAvatarData)

fun RoomInfo.toSelectRoomInfo(heroAvatarData: ImmutableList<AvatarData> = persistentListOf()) = SelectRoomInfo(
    roomId = id,
    name = name,
    avatarUrl = avatarUrl,
    heroes = heroes.withoutBridgeBotHeroes().toImmutableList(),
    heroAvatarData = heroAvatarData,
    canonicalAlias = canonicalAlias,
    isDm = isDm,
    isTombstoned = successorRoom != null,
)
