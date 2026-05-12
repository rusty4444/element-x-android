/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.messages.impl.attachments.Attachment
import io.element.android.features.messages.impl.attachments.video.MediaOptimizationSelectorPresenter
import io.element.android.libraries.androidutils.file.TemporaryUriDeleter
import io.element.android.libraries.androidutils.file.safeDelete
import io.element.android.libraries.androidutils.hash.hash
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.coroutine.firstInstanceOf
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeImage
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeVideo
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilder
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfig
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfigProvider
import io.element.android.libraries.mediaupload.api.MediaSenderFactory
import io.element.android.libraries.mediaupload.api.MediaUploadInfo
import io.element.android.libraries.mediaupload.api.allFiles
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.libraries.textcomposer.model.TextEditorState
import io.element.android.libraries.textcomposer.model.rememberMarkdownTextEditorState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class AttachmentsPreviewPresenter(
    @Assisted private val attachments: ImmutableList<Attachment>,
    @Assisted private val onDoneListener: OnDoneListener,
    @Assisted private val timelineMode: Timeline.Mode,
    @Assisted private val inReplyToEventId: EventId?,
    mediaSenderFactory: MediaSenderFactory,
    private val permalinkBuilder: PermalinkBuilder,
    private val temporaryUriDeleter: TemporaryUriDeleter,
    private val mediaOptimizationSelectorPresenterFactory: MediaOptimizationSelectorPresenter.Factory,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val mediaOptimizationConfigProvider: MediaOptimizationConfigProvider,
) : Presenter<AttachmentsPreviewState> {
    @AssistedFactory
    interface Factory {
        fun create(
            attachments: ImmutableList<Attachment>,
            timelineMode: Timeline.Mode,
            onDoneListener: OnDoneListener,
            inReplyToEventId: EventId?,
        ): AttachmentsPreviewPresenter
    }

    private val mediaSender = mediaSenderFactory.create(timelineMode)

    @Composable
    override fun present(): AttachmentsPreviewState {
        val coroutineScope = rememberCoroutineScope()

        val sendActionState = remember {
            mutableStateOf<SendActionState>(SendActionState.Idle)
        }

        val markdownTextEditorState = rememberMarkdownTextEditorState(initialText = null, initialFocus = false)
        val textEditorState by rememberUpdatedState(
            TextEditorState.Markdown(markdownTextEditorState, isRoomEncrypted = null)
        )

        val ongoingSendAttachmentJob = remember { mutableStateOf<Job?>(null) }

        var preprocessMediaJobs by remember { mutableStateOf<List<Job>>(emptyList()) }

        var selectedIndex by remember { mutableStateOf(0) }
        val currentAttachment = attachments.getOrElse(selectedIndex) { attachments.first() }
        val currentMediaAttachment = currentAttachment as Attachment.Media

        // Media optimization selector uses the first attachment for settings
        val mediaOptimizationSelectorPresenter = remember(attachments) {
            val firstMedia = attachments[0] as Attachment.Media
            mediaOptimizationSelectorPresenterFactory.create(firstMedia.localMedia)
        }
        val mediaOptimizationSelectorState by rememberUpdatedState(mediaOptimizationSelectorPresenter.present())

        val observableSendState = snapshotFlow { sendActionState.value }

        var displayFileTooLargeError by remember { mutableStateOf(false) }

        // Check all attachments for file-too-large
        LaunchedEffect(currentMediaAttachment.localMedia, mediaOptimizationSelectorState.displayMediaSelectorViews) {
            val isImageFile = currentMediaAttachment.localMedia.info.mimeType.isMimeTypeImage()
            val isVideoFile = currentMediaAttachment.localMedia.info.mimeType.isMimeTypeVideo()
            if (!isImageFile && !isVideoFile && mediaOptimizationSelectorState.maxUploadSize.dataOrNull() != null) {
                val maxUploadSize = mediaOptimizationSelectorState.maxUploadSize.dataOrNull()!!
                val fileSize = currentMediaAttachment.localMedia.info.fileSize ?: 0L
                if (maxUploadSize < fileSize) {
                    displayFileTooLargeError = true
                    return@LaunchedEffect
                }
            }
            displayFileTooLargeError = false
        }

        fun handleEvent(event: AttachmentsPreviewEvent) {
            when (event) {
                is AttachmentsPreviewEvent.SelectIndex -> {
                    selectedIndex = event.index
                }
                is AttachmentsPreviewEvent.SendAttachments -> {
                    ongoingSendAttachmentJob.value = coroutineScope.launch {
                        val config = if (mediaOptimizationSelectorState.displayMediaSelectorViews == true) {
                            MediaOptimizationConfig(
                                compressImages = mediaOptimizationSelectorState.isImageOptimizationEnabled == true,
                                videoCompressionPreset = mediaOptimizationSelectorState.selectedVideoPreset ?: VideoCompressionPreset.STANDARD,
                            )
                        } else {
                            mediaOptimizationConfigProvider.get()
                        }

                        val caption = markdownTextEditorState.getMessageMarkdown(permalinkBuilder)
                            .takeIf { it.isNotEmpty() }

                        // Process and send all attachments sequentially
                        val newJobs = attachments.map { attachment ->
                            launch(dispatchers.io) {
                                (attachment as? Attachment.Media)?.let { media ->
                                    val configForUpload = if (!media.localMedia.info.mimeType.isMimeTypeImage() &&
                                        !media.localMedia.info.mimeType.isMimeTypeVideo()
                                    ) {
                                        mediaOptimizationConfigProvider.get()
                                    } else {
                                        config
                                    }
                                    sendAttachment(
                                        mediaAttachment = media,
                                        mediaOptimizationConfig = configForUpload,
                                        caption = caption,
                                        sendActionState = sendActionState,
                                        inReplyToEventId = inReplyToEventId,
                                        onDone = { onDoneListener() },
                                    )
                                }
                            }
                        }
                        preprocessMediaJobs = newJobs
                    }
                }
                AttachmentsPreviewEvent.CancelAndDismiss -> {
                    displayFileTooLargeError = false
                    preprocessMediaJobs.forEach { it.cancel() }
                    mediaSender.cleanUp()
                    ongoingSendAttachmentJob.value?.cancel()
                    dismissAll(sendActionState)
                }
                AttachmentsPreviewEvent.CancelAndClearSendState -> {
                    ongoingSendAttachmentJob.value?.let {
                        it.cancel()
                        ongoingSendAttachmentJob.value = null
                    }
                    val mediaUploadInfo = sendActionState.value.mediaUploadInfo()
                    sendActionState.value = if (mediaUploadInfo != null) {
                        SendActionState.Sending.ReadyToUpload(mediaUploadInfo)
                    } else {
                        SendActionState.Idle
                    }
                }
            }
        }

        return AttachmentsPreviewState(
            attachments = attachments,
            selectedIndex = selectedIndex,
            sendActionState = sendActionState.value,
            textEditorState = textEditorState,
            mediaOptimizationSelectorState = mediaOptimizationSelectorState,
            displayFileTooLargeError = displayFileTooLargeError,
            eventSink = ::handleEvent,
        )
    }

    private suspend fun sendAttachment(
        mediaAttachment: Attachment.Media,
        mediaOptimizationConfig: MediaOptimizationConfig,
        caption: String?,
        sendActionState: MutableState<SendActionState>,
        inReplyToEventId: EventId?,
        onDone: () -> Unit,
    ) = runCatchingExceptions {
        sendActionState.value = SendActionState.Sending.Processing(displayProgress = true)
        mediaSender.preProcessMedia(
            uri = mediaAttachment.localMedia.uri,
            mimeType = mediaAttachment.localMedia.info.mimeType,
            mediaOptimizationConfig = mediaOptimizationConfig,
        ).getOrThrow().also { mediaUploadInfo ->
            sendActionState.value = SendActionState.Sending.Uploading(mediaUploadInfo)
            mediaSender.sendPreProcessedMedia(
                mediaUploadInfo = mediaUploadInfo,
                caption = caption,
                formattedCaption = null,
                inReplyToEventId = inReplyToEventId,
            ).getOrThrow()
            mediaUploadInfo.allFiles().forEach { file -> file.safeDelete() }
            temporaryUriDeleter.delete(mediaAttachment.localMedia.uri)
        }
    }.fold(
        onSuccess = {
            sendActionState.value = SendActionState.Done
            onDone()
        },
        onFailure = { error ->
            Timber.e(error, "Failed to send attachment")
            if (error is CancellationException) {
                throw error
            } else {
                sendActionState.value = SendActionState.Failure(error, null)
            }
        },
    )

    private fun dismissAll(sendActionState: MutableState<SendActionState>) {
        attachments.forEach { attachment ->
            if (attachment is Attachment.Media) {
                temporaryUriDeleter.delete(attachment.localMedia.uri)
            }
        }
        sendActionState.value = SendActionState.Done
        onDoneListener()
    }
}
