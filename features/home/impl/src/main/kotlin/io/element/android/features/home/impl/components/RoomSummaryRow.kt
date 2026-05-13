/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.model.LatestEvent
import io.element.android.features.home.impl.model.RoomBridgeBadge
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomListRoomSummaryProvider
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.roomlist.RoomListEvent
import io.element.android.libraries.core.extensions.orEmpty
import io.element.android.libraries.core.extensions.toSafeLength
import io.element.android.libraries.designsystem.atomic.atoms.UnreadIndicatorAtom
import io.element.android.libraries.designsystem.atomic.molecules.InviteButtonsRowMolecule
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.roomListRoomMessage
import io.element.android.libraries.designsystem.theme.roomListRoomMessageDate
import io.element.android.libraries.designsystem.theme.roomListRoomName
import io.element.android.libraries.designsystem.theme.unreadIndicator
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.ui.components.InviteSenderView
import io.element.android.libraries.matrix.ui.model.InviteSender
import io.element.android.libraries.ui.strings.CommonStrings
import timber.log.Timber

internal val minHeight = 84.dp

@Composable
internal fun RoomSummaryRow(
    room: RoomListRoomSummary,
    hideInviteAvatars: Boolean,
    isInviteSeen: Boolean,
    onClick: (RoomListRoomSummary) -> Unit,
    eventSink: (RoomListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (room.displayType) {
            RoomSummaryDisplayType.PLACEHOLDER -> {
                RoomSummaryPlaceholderRow()
            }
            RoomSummaryDisplayType.INVITE -> {
                RoomSummaryScaffoldRow(
                    room = room,
                    hideAvatarImage = hideInviteAvatars,
                    onClick = onClick,
                    onLongClick = {
                        Timber.d("Long click on invite room")
                    },
                ) {
                    InviteNameAndIndicatorRow(name = room.name, isInviteSeen = isInviteSeen)
                    InviteSubtitle(isDm = room.isDm, inviteSender = room.inviteSender)
                    if (!room.isDm && room.inviteSender != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        InviteSenderView(
                            modifier = Modifier.fillMaxWidth(),
                            inviteSender = room.inviteSender,
                            hideAvatarImage = hideInviteAvatars
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InviteButtonsRowMolecule(
                        onAcceptClick = {
                            eventSink(RoomListEvent.AcceptInvite(room))
                        },
                        onDeclineClick = {
                            eventSink(RoomListEvent.ShowDeclineInviteMenu(room))
                        }
                    )
                }
            }
            RoomSummaryDisplayType.ROOM -> {
                RoomSummaryScaffoldRow(
                    room = room,
                    onClick = onClick,
                    onLongClick = {
                        eventSink(RoomListEvent.ShowContextMenu(room))
                    },
                ) {
                    NameAndTimestampRow(
                        name = room.name,
                        timestamp = room.timestamp,
                        bridgeBadge = room.bridgeBadge,
                        isHighlighted = room.isHighlighted
                    )
                    MessagePreviewAndIndicatorRow(room = room)
                }
            }
            RoomSummaryDisplayType.KNOCKED -> {
                RoomSummaryScaffoldRow(
                    room = room,
                    onClick = onClick,
                    onLongClick = {
                        Timber.d("Long click on knocked room")
                    },
                ) {
                    NameAndTimestampRow(
                        name = room.name,
                        timestamp = null,
                        bridgeBadge = room.bridgeBadge,
                        isHighlighted = room.isHighlighted
                    )
                    if (room.canonicalAlias != null) {
                        Text(
                            text = room.canonicalAlias.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ElementTheme.typography.fontBodyMdRegular,
                            color = ElementTheme.colors.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = stringResource(id = R.string.screen_roomlist_knock_event_sent_description),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomSummaryScaffoldRow(
    room: RoomListRoomSummary,
    onClick: (RoomListRoomSummary) -> Unit,
    onLongClick: (RoomListRoomSummary) -> Unit,
    modifier: Modifier = Modifier,
    hideAvatarImage: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = Modifier
        .combinedClickable(
            onClick = { onClick(room) },
            onLongClick = { onLongClick(room) },
            onLongClickLabel = stringResource(CommonStrings.action_open_context_menu),
            indication = ripple(),
            interactionSource = remember { MutableInteractionSource() }
        )
        .onKeyboardContextMenuAction { onLongClick(room) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .height(IntrinsicSize.Min),
    ) {
        Avatar(
            avatarData = room.avatarData,
            avatarType = if (room.isSpace) {
                AvatarType.Space(isTombstoned = room.isTombstoned)
            } else {
                AvatarType.Room(
                    heroes = room.heroes,
                    isTombstoned = room.isTombstoned,
                )
            },
            hideImage = hideAvatarImage,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun NameAndTimestampRow(
    name: String?,
    timestamp: String?,
    bridgeBadge: RoomBridgeBadge,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = spacedBy(16.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            style = ElementTheme.typography.fontBodyLgMedium,
            text = name?.toSafeLength(ellipsize = true) ?: stringResource(id = CommonStrings.common_no_room_name),
            fontStyle = FontStyle.Italic.takeIf { name == null },
            color = ElementTheme.colors.roomListRoomName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (timestamp != null) {
            // Timestamp
            Text(
                text = timestamp,
                style = ElementTheme.typography.fontBodySmMedium,
                color = if (isHighlighted) {
                    ElementTheme.colors.unreadIndicator
                } else {
                    ElementTheme.colors.roomListRoomMessageDate
                },
            )
            // Badge positioned to the right of timestamp
            if (bridgeBadge != RoomBridgeBadge.MATRIX) {
                Spacer(modifier = Modifier.width(4.dp))
                RoomBridgeBadgeView(bridgeBadge = bridgeBadge)
            }
        }
    }
}

@Composable
private fun RoomBridgeBadgeView(
    bridgeBadge: RoomBridgeBadge,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .background(
                color = bridgeBadge.backgroundColor,
                shape = RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = bridgeBadge.logoText,
            style = ElementTheme.typography.fontBodySmMedium,
            color = bridgeBadge.foregroundColor,
            maxLines = 1,
        )
    }
}

private val RoomBridgeBadge.logoText: String
    get() = when (this) {
        RoomBridgeBadge.WHATSAPP -> "☎"
        RoomBridgeBadge.META -> "∞"
        RoomBridgeBadge.GMESSAGES -> "G"
        RoomBridgeBadge.MATRIX -> "✦"
        RoomBridgeBadge.X -> "𝕏"
        RoomBridgeBadge.DISCORD -> "D"
        RoomBridgeBadge.LINKEDIN -> "in"
        RoomBridgeBadge.TELEGRAM -> "✈"
        RoomBridgeBadge.SIGNAL -> "S"
        RoomBridgeBadge.SLACK -> "#"
        RoomBridgeBadge.INSTAGRAM -> "◎"
        RoomBridgeBadge.IMESSAGE -> "i"
        RoomBridgeBadge.GENERIC_BRIDGE -> "↔"
    }

private val RoomBridgeBadge.backgroundColor: Color
    get() = when (this) {
        RoomBridgeBadge.WHATSAPP -> Color(0xFF25D366)
        RoomBridgeBadge.META -> Color(0xFF0866FF)
        RoomBridgeBadge.GMESSAGES -> Color(0xFF1A73E8)
        RoomBridgeBadge.MATRIX -> Color(0xFF000000)
        RoomBridgeBadge.X -> Color(0xFF000000)
        RoomBridgeBadge.DISCORD -> Color(0xFF5865F2)
        RoomBridgeBadge.LINKEDIN -> Color(0xFF0A66C2)
        RoomBridgeBadge.TELEGRAM -> Color(0xFF229ED9)
        RoomBridgeBadge.SIGNAL -> Color(0xFF3A76F0)
        RoomBridgeBadge.SLACK -> Color(0xFF611F69)
        RoomBridgeBadge.INSTAGRAM -> Color(0xFFE4405F)
        RoomBridgeBadge.IMESSAGE -> Color(0xFF34C759)
        RoomBridgeBadge.GENERIC_BRIDGE -> Color(0xFF6B7280)
    }

private val RoomBridgeBadge.foregroundColor: Color
    get() = Color.White

@Composable
private fun InviteSubtitle(
    isDm: Boolean,
    inviteSender: InviteSender?,
    modifier: Modifier = Modifier
) {
    val subtitle = if (isDm) {
        inviteSender?.userId?.value
    } else {
        null
    }
    if (subtitle != null) {
        Text(
            modifier = modifier.clipToBounds(),
            text = subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.roomListRoomMessage,
        )
    }
}

@Composable
private fun MessagePreviewAndIndicatorRow(
    room: RoomListRoomSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (room.isTombstoned) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.screen_roomlist_tombstoned_room_description),
                color = ElementTheme.colors.roomListRoomMessage,
                style = ElementTheme.typography.fontBodyMdRegular,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            if (room.latestEvent is LatestEvent.Error) {
                Icon(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                    imageVector = CompoundIcons.ErrorSolid(),
                    // The last message contains the error.
                    contentDescription = null,
                    tint = ElementTheme.colors.iconCriticalPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(CommonStrings.common_message_failed_to_send),
                    color = ElementTheme.colors.textCriticalPrimary,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                if (room.latestEvent is LatestEvent.Sending) {
                    Icon(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                        imageVector = CompoundIcons.Time(),
                        contentDescription = stringResource(CommonStrings.common_sending),
                        tint = ElementTheme.colors.iconTertiary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                val messagePreview = room.latestEvent.content()
                val annotatedMessagePreview = messagePreview as? AnnotatedString ?: AnnotatedString(text = messagePreview.orEmpty().toString())
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds(),
                    text = annotatedMessagePreview,
                    color = ElementTheme.colors.roomListRoomMessage,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Call and unread
        Row(
            modifier = Modifier
                .heightIn(min = 20.dp)
                // Used to force this line to be read aloud earlier than the latest event when using Talkback
                .zIndex(-1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (room.isHighlighted) ElementTheme.colors.unreadIndicator else ElementTheme.colors.iconQuaternary
            if (room.hasRoomCall) {
                OnGoingCallIcon(
                    color = tint,
                    isAudio = room.activeCallIntent == CallIntent.AUDIO
                )
            }
            if (room.userDefinedNotificationMode == RoomNotificationMode.MUTE) {
                NotificationOffIndicatorAtom()
            } else if (room.numberOfUnreadMentions > 0) {
                MentionIndicatorAtom()
            }
            if (room.hasNewContent) {
                val unreadCount = room.numberOfUnreadMessages.takeIf { it > 0 }
                    ?: room.numberOfUnreadNotifications.takeIf { it > 0 }
                val contentDescription = if (unreadCount != null) {
                    stringResource(CommonStrings.a11y_notifications_new_messages) + ": $unreadCount"
                } else {
                    stringResource(CommonStrings.a11y_notifications_new_messages)
                }
                UnreadCountBadge(
                    count = unreadCount,
                    color = tint,
                    contentDescription = contentDescription,
                    isMarkedUnread = room.isMarkedUnread,
                )
            }
        }
    }
}

@Composable
private fun InviteNameAndIndicatorRow(
    name: String?,
    isInviteSeen: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            style = ElementTheme.typography.fontBodyLgMedium,
            text = name?.toSafeLength(ellipsize = true) ?: stringResource(id = CommonStrings.common_no_room_name),
            fontStyle = FontStyle.Italic.takeIf { name == null },
            color = ElementTheme.colors.roomListRoomName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!isInviteSeen) {
            UnreadIndicatorAtom(
                color = ElementTheme.colors.unreadIndicator
            )
        }
    }
}

@Composable
private fun OnGoingCallIcon(
    color: Color,
    isAudio: Boolean
) {
    Icon(
        modifier = Modifier.size(16.dp),
        imageVector = if (isAudio) CompoundIcons.VoiceCallSolid() else CompoundIcons.VideoCallSolid(),
        contentDescription = stringResource(CommonStrings.a11y_notifications_ongoing_call),
        tint = color,
    )
}

@Composable
private fun NotificationOffIndicatorAtom() {
    Icon(
        modifier = Modifier.size(16.dp),
        contentDescription = stringResource(CommonStrings.a11y_notifications_muted),
        imageVector = CompoundIcons.NotificationsOffSolid(),
        tint = ElementTheme.colors.iconQuaternary,
    )
}

@Composable
private fun MentionIndicatorAtom() {
    Icon(
        modifier = Modifier.size(16.dp),
        contentDescription = stringResource(CommonStrings.a11y_notifications_new_mentions),
        imageVector = CompoundIcons.Mention(),
        tint = ElementTheme.colors.unreadIndicator,
    )
}

/**
 * Beeper-style unread count badge.
 * Shows a numeric count inside a colored pill when available, otherwise shows a dot.
 * Falls back to a dot for manually marked-unread rooms.
 */
@Composable
private fun UnreadCountBadge(
    count: Long?,
    color: Color,
    contentDescription: String,
    isMarkedUnread: Boolean,
) {
    val hasCount = count != null && !isMarkedUnread
    if (hasCount) {
        // Use the accent color from the theme for the badge background
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .heightIn(min = 18.dp)
                .widthIn(min = 18.dp)
                .background(
                    color = ElementTheme.colors.bgBadgeAccent,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = ElementTheme.colors.textBadgeAccent,
                style = ElementTheme.typography.fontBodySmMedium,
                modifier = Modifier.padding(horizontal = 5.dp),
            )
        }
    } else {
        // Dot for marked-unread or when no count available
        UnreadIndicatorAtom(
            color = color,
            contentDescription = contentDescription,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun RoomSummaryRowPreview(@PreviewParameter(RoomListRoomSummaryProvider::class) data: RoomListRoomSummary) = ElementPreview {
    RoomSummaryRow(
        room = data,
        hideInviteAvatars = false,
        // Set isInviteSeen to true for the preview when the room has name "Bob"
        isInviteSeen = data.name == "Bob",
        onClick = {},
        eventSink = {},
    )
}
