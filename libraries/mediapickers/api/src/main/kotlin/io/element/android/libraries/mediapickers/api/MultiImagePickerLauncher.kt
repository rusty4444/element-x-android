/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediapickers.api

import android.content.ActivityNotFoundException
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import timber.log.Timber

/**
 * Wrapper for multi-image picker launcher.
 */
interface MultiImagePickerLauncher {
    fun launch()
}

/** Compose-backed launcher that uses ActivityResultContracts.PickMultipleVisualMedia. */
class ComposeMultiImagePickerLauncher(
    private val managedLauncher: (PickVisualMediaRequest) -> Unit,
) : MultiImagePickerLauncher {
    override fun launch() {
        try {
            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            managedLauncher(request)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Timber.w(activityNotFoundException, "No activity found for multi-image picker")
        }
    }
}

/** Needed for screenshot tests. */
class NoOpMultiImagePickerLauncher : MultiImagePickerLauncher {
    override fun launch() = Unit
}
