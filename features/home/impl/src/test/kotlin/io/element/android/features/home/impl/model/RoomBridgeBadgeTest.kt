/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.model

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomAlias
import org.junit.Test

class RoomBridgeBadgeTest {
    @Test
    fun `detects bridge badges from member user IDs`() {
        assertThat(RoomBridgeBadge.from(roomName = "Alice", aliases = emptyList(), heroUserIds = listOf("@meta_123:bridge.example")))
            .isEqualTo(RoomBridgeBadge.META)
        assertThat(RoomBridgeBadge.from(roomName = "Bob", aliases = emptyList(), heroUserIds = listOf("@gmessages_123:bridge.example")))
            .isEqualTo(RoomBridgeBadge.GMESSAGES)
        assertThat(RoomBridgeBadge.from(roomName = "Carol", aliases = emptyList(), heroUserIds = listOf("@linkedin_123:bridge.example")))
            .isEqualTo(RoomBridgeBadge.LINKEDIN)
    }

    @Test
    fun `detects bridge badges from room aliases`() {
        assertThat(RoomBridgeBadge.from(roomName = "Announcements", aliases = listOf(RoomAlias("#mautrix-whatsapp_123:example.org"))))
            .isEqualTo(RoomBridgeBadge.WHATSAPP)
    }

    @Test
    fun `defaults to matrix badge for native rooms`() {
        assertThat(RoomBridgeBadge.from(roomName = "Native Matrix Room", aliases = emptyList(), heroUserIds = listOf("@alice:matrix.org")))
            .isEqualTo(RoomBridgeBadge.MATRIX)
    }
}
