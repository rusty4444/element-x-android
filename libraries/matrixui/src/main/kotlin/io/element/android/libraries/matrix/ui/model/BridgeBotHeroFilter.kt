/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.model

import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.user.MatrixUser

private const val BRIDGE_BOT_NAME_MARKER = "bridge"

fun MatrixUser.isBridgeBotHero(): Boolean {
    return displayName.containsBridgeMarker() || userId.value.containsBridgeMarker()
}

fun RoomMember.isBridgeBotHero(): Boolean {
    return displayName.containsBridgeMarker() || userId.value.containsBridgeMarker()
}

fun Iterable<MatrixUser>.withoutBridgeBotHeroes(): List<MatrixUser> {
    return filterNot { user -> user.isBridgeBotHero() }
}

fun Sequence<RoomMember>.withoutBridgeBotHeroes(): Sequence<RoomMember> {
    return filterNot { member -> member.isBridgeBotHero() }
}

private fun String?.containsBridgeMarker(): Boolean {
    return this?.contains(BRIDGE_BOT_NAME_MARKER, ignoreCase = true) == true
}
