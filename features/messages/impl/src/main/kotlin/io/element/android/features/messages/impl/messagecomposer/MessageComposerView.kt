/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerEvent
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerState
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerStateProvider
import io.element.android.features.messages.api.timeline.voicemessages.composer.aVoiceMessageComposerState
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.scheduledsend.ScheduledMessageInfo
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.textcomposer.TextComposer
import io.element.android.libraries.textcomposer.model.Suggestion
import io.element.android.libraries.textcomposer.model.VoiceMessagePlayerEvent
import io.element.android.libraries.textcomposer.model.VoiceMessageRecorderEvent
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ScheduledMessageBanner(
    scheduledMessages: ImmutableList<ScheduledMessageInfo>,
    onCancel: (ScheduledMessageInfo) -> Unit,
    onForceSend: (ScheduledMessageInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        scheduledMessages.forEach { info ->
            ScheduledMessageBubble(
                info = info,
                timeText = timeFormatter.format(Date(info.scheduledTimeMillis)),
                onCancel = { onCancel(info) },
                onForceSend = { onForceSend(info) },
            )
        }
    }
}

@Composable
private fun ScheduledMessageBubble(
    info: ScheduledMessageInfo,
    timeText: String,
    onCancel: () -> Unit,
    onForceSend: () -> Unit,
) {
    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) }
    val accentColor = ElementTheme.colors.textActionAccent
    val criticalColor = ElementTheme.colors.textCriticalPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElementTheme.colors.bgSubtleSecondary),
    ) {
        // Draw dashed border via Canvas overlay
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3f
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            radiusX = 12.dp.toPx(),
                            radiusY = 12.dp.toPx(),
                        )
                    )
                }
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = dashPathEffect,
                    ),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Scheduled time label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CompoundIcons.Time(),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ElementTheme.colors.textActionAccent,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = timeText,
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textActionAccent,
                )
                Spacer(Modifier.weight(1f))
            }
            // Message preview
            Text(
                text = info.formattedPreview(),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(R.string.action_send),
                    onClick = onForceSend,
                    leadingIcon = IconSource.Vector(CompoundIcons.SendSolid()),
                    size = io.element.android.libraries.designsystem.theme.components.ButtonSize.Medium,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = CompoundIcons.Close(),
                        contentDescription = stringResource(CommonStrings.action_cancel),
                        modifier = Modifier.size(18.dp),
                        tint = ElementTheme.colors.textCriticalPrimary,
                    )
                }
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun MessageComposerView(
    state: MessageComposerState,
    voiceMessageState: VoiceMessageComposerState,
    onScheduleMessage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    fun sendMessage() {
        state.eventSink(MessageComposerEvent.SendMessage)
    }

    fun sendUri(uri: Uri) {
        state.eventSink(MessageComposerEvent.SendUri(uri))
    }

    fun onAddAttachment() {
        state.eventSink(MessageComposerEvent.AddAttachment)
    }

    fun onCloseSpecialMode() {
        state.eventSink(MessageComposerEvent.CloseSpecialMode)
    }

    fun onDismissTextFormatting() {
        view.clearFocus()
        state.eventSink(MessageComposerEvent.ToggleTextFormatting(enabled = false))
    }

    fun onSuggestionReceived(suggestion: Suggestion?) {
        state.eventSink(MessageComposerEvent.SuggestionReceived(suggestion))
    }

    fun onError(error: Throwable) {
        state.eventSink(MessageComposerEvent.Error(error))
    }

    fun onTyping(typing: Boolean) {
        state.eventSink(MessageComposerEvent.TypingNotice(typing))
    }

    val coroutineScope = rememberCoroutineScope()
    fun onRequestFocus() {
        coroutineScope.launch {
            state.textEditorState.requestFocus()
        }
    }

    val onVoiceRecorderEvent = { press: VoiceMessageRecorderEvent ->
        voiceMessageState.eventSink(VoiceMessageComposerEvent.RecorderEvent(press))
    }

    val onSendVoiceMessage = {
        voiceMessageState.eventSink(VoiceMessageComposerEvent.SendVoiceMessage)
        state.eventSink(MessageComposerEvent.CloseSpecialMode)
    }

    val onDeleteVoiceMessage = {
        voiceMessageState.eventSink(VoiceMessageComposerEvent.DeleteVoiceMessage)
    }

    val onVoicePlayerEvent = { event: VoiceMessagePlayerEvent ->
        voiceMessageState.eventSink(VoiceMessageComposerEvent.PlayerEvent(event))
    }

    Column(modifier = modifier) {
        // Scheduled message banner
        if (state.scheduledMessageInfos.isNotEmpty()) {
            ScheduledMessageBanner(
                scheduledMessages = state.scheduledMessageInfos,
                onCancel = { info -> state.eventSink(MessageComposerEvent.CancelScheduledMessage(info)) },
                onForceSend = { info -> state.eventSink(MessageComposerEvent.ForceSendScheduledMessage(info)) },
            )
        }

        TextComposer(
            modifier = Modifier.height(IntrinsicSize.Min),
            state = state.textEditorState,
            voiceMessageState = voiceMessageState.voiceMessageState,
            onRequestFocus = ::onRequestFocus,
            onSendMessage = ::sendMessage,
            onScheduleMessage = onScheduleMessage,
            composerMode = state.mode,
            showTextFormatting = state.showTextFormatting,
            onResetComposerMode = ::onCloseSpecialMode,
            onAddAttachment = ::onAddAttachment,
            onDismissTextFormatting = ::onDismissTextFormatting,
            onVoiceRecorderEvent = onVoiceRecorderEvent,
            onVoicePlayerEvent = onVoicePlayerEvent,
            onSendVoiceMessage = onSendVoiceMessage,
            onDeleteVoiceMessage = onDeleteVoiceMessage,
            onReceiveSuggestion = ::onSuggestionReceived,
            resolveMentionDisplay = state.resolveMentionDisplay,
            resolveAtRoomMentionDisplay = state.resolveAtRoomMentionDisplay,
            onError = ::onError,
            onTyping = ::onTyping,
            onSelectRichContent = ::sendUri,
        )

        AsyncActionView(
            async = state.slashCommandAction,
            onSuccess = {},
            onErrorDismiss = { state.eventSink(MessageComposerEvent.ClearSlashError) },
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MessageComposerViewPreview(
    @PreviewParameter(MessageComposerStateProvider::class) state: MessageComposerState,
) = ElementPreview {
    Column {
        MessageComposerView(
            modifier = Modifier.height(IntrinsicSize.Min),
            state = state,
            voiceMessageState = aVoiceMessageComposerState(),
        )
        MessageComposerView(
            modifier = Modifier.height(200.dp),
            state = state,
            voiceMessageState = aVoiceMessageComposerState(),
        )
        DisabledComposerView()
    }
}

@PreviewsDayNight
@Composable
internal fun MessageComposerViewVoicePreview(
    @PreviewParameter(VoiceMessageComposerStateProvider::class) state: VoiceMessageComposerState,
) = ElementPreview {
    Column {
        MessageComposerView(
            modifier = Modifier.height(IntrinsicSize.Min),
            state = aMessageComposerState(),
            voiceMessageState = state,
        )
    }
}
