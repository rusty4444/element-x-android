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
import io.element.android.features.messages.impl.attachments.preview.imageeditor.AttachmentImageEdits
import io.element.android.features.messages.impl.attachments.preview.imageeditor.AttachmentImageEditor
import io.element.android.features.messages.impl.attachments.preview.imageeditor.AttachmentImageEditorState
import io.element.android.features.messages.impl.attachments.preview.imageeditor.EditedLocalMedia
import io.element.android.features.messages.impl.attachments.video.MediaOptimizationSelectorPresenter
import io.element.android.libraries.androidutils.file.TemporaryUriDeleter
import io.element.android.libraries.androidutils.file.safeDelete
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeImage
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeVideo
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilder
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfig
import io.element.android.libraries.mediaupload.api.MediaOptimizationConfigProvider
import io.element.android.libraries.mediaupload.api.MediaSenderFactory
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
    private val attachmentImageEditor: AttachmentImageEditor,
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

        var selectedIndex by remember { mutableStateOf(0) }
        val currentAttachment = attachments.getOrElse(selectedIndex) { attachments.first() }
        val currentMediaAttachment = currentAttachment as Attachment.Media

        // Image editor state — only relevant for single image
        var imageEditorState by remember { mutableStateOf<AttachmentImageEditorState?>(null) }
        var pendingEdits by remember { mutableStateOf<AttachmentImageEdits?>(null) }
        var editedMedia by remember { mutableStateOf<EditedLocalMedia?>(null) }
        var isApplyingImageEdits by remember { mutableStateOf(false) }
        var displayImageEditError by remember { mutableStateOf(false) }

        // Editing is supported for single image only, and only for still images
        val isSingleImage = attachments.size == 1 && currentMediaAttachment.localMedia.info.isImageAttachment()
        val canEditImage = isSingleImage && currentMediaAttachment.localMedia.info.canEditImage()

        // Media optimization selector uses the first attachment for settings
        val mediaOptimizationSelectorPresenter = remember(attachments) {
            val firstMedia = attachments[0] as Attachment.Media
            mediaOptimizationSelectorPresenterFactory.create(firstMedia.localMedia, sendAsFile = false)
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

                        // Send all attachments sequentially
                        for (attachment in attachments) {
                            if (!isActive) return@launch
                            (attachment as? Attachment.Media)?.let { media ->
                                val configForUpload = if (!media.localMedia.info.mimeType.isMimeTypeImage() &&
                                    !media.localMedia.info.mimeType.isMimeTypeVideo()
                                ) {
                                    mediaOptimizationConfigProvider.get()
                                } else {
                                    config
                                }
                                // If this is a single image with edits already applied, use edited version
                                val editedAttachment = if (attachments.size == 1 && editedMedia != null) {
                                    Attachment.Media(editedMedia!!.localMedia)
                                } else {
                                    null
                                }
                                sendAttachment(
                                    mediaAttachment = editedAttachment ?: media,
                                    mediaOptimizationConfig = configForUpload,
                                    caption = caption,
                                    sendActionState = sendActionState,
                                    inReplyToEventId = inReplyToEventId,
                                )
                            }
                        }
                        if (isActive) onDoneListener()
                    }
                }
                AttachmentsPreviewEvent.CancelAndDismiss -> {
                    displayFileTooLargeError = false
                    imageEditorState = null
                    pendingEdits = null
                    editedMedia?.file?.safeDelete()
                    editedMedia = null
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
                // Image editor events
                AttachmentsPreviewEvent.OpenImageEditor -> {
                    imageEditorState = AttachmentImageEditorState(
                        localMedia = currentMediaAttachment.localMedia,
                        edits = pendingEdits ?: AttachmentImageEdits(),
                        previewDebug = false,
                    )
                }
                AttachmentsPreviewEvent.CloseImageEditor -> {
                    imageEditorState = null
                    // Don't clear pendingEdits — user might send with existing edits
                }
                AttachmentsPreviewEvent.RotateImageToTheLeft -> {
                    imageEditorState = imageEditorState?.copy(
                        edits = imageEditorState!!.edits.rotateAntiClockwise()
                    )
                }
                AttachmentsPreviewEvent.FlipImageHorizontally -> {
                    imageEditorState = imageEditorState?.copy(
                        edits = imageEditorState!!.edits.flipHorizontally()
                    )
                }
                AttachmentsPreviewEvent.FlipImageVertically -> {
                    imageEditorState = imageEditorState?.copy(
                        edits = imageEditorState!!.edits.flipVertically()
                    )
                }
                AttachmentsPreviewEvent.ApplyImageEdits -> {
                    // Export edits immediately so the preview updates
                    val edits = imageEditorState?.edits?.takeIf { it.hasChanges }
                    imageEditorState = null
                    if (edits != null) {
                        isApplyingImageEdits = true
                        ongoingSendAttachmentJob.value = coroutineScope.launch {
                            val result = attachmentImageEditor.exportEdits(
                                localMedia = currentMediaAttachment.localMedia,
                                edits = edits,
                            )
                            isApplyingImageEdits = false
                            result.fold(
                                onSuccess = {
                                    // Clean up previous edited file
                                    editedMedia?.file?.safeDelete()
                                    editedMedia = it
                                    pendingEdits = edits
                                },
                                onFailure = { error ->
                                    Timber.e(error, "Failed to apply image edits")
                                    displayImageEditError = true
                                    pendingEdits = null
                                    editedMedia = null
                                }
                            )
                        }
                    } else {
                        pendingEdits = null
                        editedMedia = null
                    }
                }
                AttachmentsPreviewEvent.ResetImageEdits -> {
                    pendingEdits = null
                    editedMedia?.file?.safeDelete()
                    editedMedia = null
                    imageEditorState = imageEditorState?.copy(
                        edits = AttachmentImageEdits()
                    )
                }
                is AttachmentsPreviewEvent.UpdateImageCropRect -> {
                    imageEditorState = imageEditorState?.copy(
                        edits = imageEditorState!!.edits.copy(cropRect = event.cropRect)
                    )
                }
                AttachmentsPreviewEvent.ClearImageEditError -> {
                    displayImageEditError = false
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
            imageEditorState = imageEditorState,
            canEditImage = canEditImage,
            hasPendingEdits = pendingEdits?.hasChanges == true,
            isApplyingImageEdits = isApplyingImageEdits,
            displayImageEditError = displayImageEditError,
            previewMedia = editedMedia?.localMedia,
        )
    }

    private suspend fun sendAttachment(
        mediaAttachment: Attachment.Media,
        mediaOptimizationConfig: MediaOptimizationConfig,
        caption: String?,
        sendActionState: MutableState<SendActionState>,
        inReplyToEventId: EventId?,
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
