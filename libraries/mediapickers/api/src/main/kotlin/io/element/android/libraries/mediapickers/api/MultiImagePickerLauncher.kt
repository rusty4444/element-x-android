/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediapickers.api

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import timber.log.Timber

/**
 * Wrapper for multi-image picker launcher.
 */
interface MultiImagePickerLauncher {
    fun launch()
}

/** Compose-backed launcher that uses ActivityResultContracts.OpenMultipleDocuments. */
class ComposeMultiImagePickerLauncher(
    private val managedLauncher: ManagedActivityResultLauncher<Nothing?, List<Uri>>,
) : MultiImagePickerLauncher {
    override fun launch() {
        try {
            managedLauncher.launch(null)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Timber.w(activityNotFoundException, "No activity found for multi-image picker")
        }
    }
}

/** Needed for screenshot tests. */
class NoOpMultiImagePickerLauncher : MultiImagePickerLauncher {
    override fun launch() = Unit
}