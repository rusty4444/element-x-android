/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediapickers.api

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * An [ActivityResultContract] that opens the system document picker for multiple
 * visual media files (images and videos).
 *
 * Unlike [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia],
 * which only shows images in multi-select mode, this contract uses
 * [Intent.ACTION_OPEN_DOCUMENT] with [Intent.EXTRA_MIME_TYPES] set to both
 * `image/*` and `video/*`, ensuring both media types appear with multi-select
 * on all Android versions.
 *
 * The input is the MIME type string to use as the primary type filter
 * (e.g. `"image/*"`). Videos are added via EXTRA_MIME_TYPES.
 */
class OpenMultipleVisualMedia : ActivityResultContract<String, List<Uri>>() {

    companion object {
        private val MIME_TYPES = arrayOf("image/*", "video/*")
    }

    override fun createIntent(context: android.content.Context, input: String): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPES)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null || resultCode != android.app.Activity.RESULT_OK) {
            return emptyList()
        }
        val uris = mutableListOf<Uri>()
        // Single selection
        intent.data?.let { uris.add(it) }
        // Multiple selection (ClipData on newer APIs)
        intent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { uris.add(it) }
            }
        }
        return uris
    }
}
