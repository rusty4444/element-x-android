/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.model

import io.element.android.libraries.matrix.api.core.RoomAlias

enum class RoomBridgeBadge {
    WHATSAPP,
    META,
    GMESSAGES,
    MATRIX,
    X,
    DISCORD,
    LINKEDIN,
    TELEGRAM,
    SIGNAL,
    SLACK,
    INSTAGRAM,
    IMESSAGE,
    GENERIC_BRIDGE,
    ;

    companion object {
        fun from(roomName: String?, aliases: List<RoomAlias>): RoomBridgeBadge {
            val haystack = buildString {
                append(roomName.orEmpty())
                aliases.forEach { alias ->
                    append(' ')
                    append(alias.value)
                }
            }.lowercase()

            return when {
                haystack.anyOf("whatsapp", "mautrix-wa") -> WHATSAPP
                haystack.anyOf("gmessages", "googlemessages", "google-messages", "google messages") -> GMESSAGES
                haystack.anyOf("discord") -> DISCORD
                haystack.anyOf("linkedin", "linked-in") -> LINKEDIN
                haystack.anyOf("twitter", "x-twitter", "mautrix-x", "x.com") -> X
                haystack.anyOf("telegram", "tg") -> TELEGRAM
                haystack.anyOf("signal") -> SIGNAL
                haystack.anyOf("slack") -> SLACK
                haystack.anyOf("instagram", "insta") -> INSTAGRAM
                haystack.anyOf("imessage", "i-message") -> IMESSAGE
                haystack.anyOf("facebook", "messenger", "meta") -> META
                haystack.anyOf("mautrix", "bridge") -> GENERIC_BRIDGE
                else -> MATRIX
            }
        }
    }
}

private fun String.anyOf(vararg needles: String): Boolean {
    return needles.any { needle -> contains(needle) }
}
