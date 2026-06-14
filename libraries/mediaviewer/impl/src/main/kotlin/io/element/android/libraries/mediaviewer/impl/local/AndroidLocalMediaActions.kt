/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.local

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.core.net.toFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.androidutils.system.startInstallFromSourceIntent
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@ContributesBinding(AppScope::class)
class AndroidLocalMediaActions(
    @ApplicationContext private val context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val buildMeta: BuildMeta,
) : LocalMediaActions {
    private var activityContext: Context? = null
    private var apkInstallLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null
    private var pendingMedia: LocalMedia? = null

    @Composable
    override fun Configure() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        apkInstallLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                pendingMedia?.let {
                    coroutineScope.launch {
                        openFile(it)
                    }
                }
            } else {
                // User cancelled
            }
            pendingMedia = null
        }
        return DisposableEffect(Unit) {
            activityContext = context
            onDispose {
                activityContext = null
            }
        }
    }

    override suspend fun saveOnDisk(localMedia: LocalMedia): Result<Unit> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveOnDiskUsingMediaStore(localMedia)
            } else {
                saveOnDiskUsingExternalStorageApi(localMedia)
            }
        }.onSuccess {
            Timber.v("Save on disk succeed")
        }.onFailure {
            Timber.e(it, "Save on disk failed for uri=${localMedia.uri} filename=${localMedia.info.filename}")
        }
    }

    override suspend fun share(localMedia: LocalMedia): Result<Unit> = withContext(coroutineDispatchers.io) {
        require(localMedia.uri.scheme == ContentResolver.SCHEME_FILE)
        runCatchingExceptions {
            val shareableUri = localMedia.toShareableUri()
            val shareMediaIntent = Intent(Intent.ACTION_SEND)
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, shareableUri)
                .setTypeAndNormalize(localMedia.info.mimeType)
            withContext(coroutineDispatchers.main) {
                val intent = Intent.createChooser(shareMediaIntent, null)
                activityContext!!.startActivity(intent)
            }
        }.onSuccess {
            Timber.v("Share media succeed")
        }.onFailure {
            Timber.e(it, "Share media failed")
        }
    }

    override suspend fun open(localMedia: LocalMedia): Result<Unit> = withContext(coroutineDispatchers.io) {
        require(localMedia.uri.scheme == ContentResolver.SCHEME_FILE)
        runCatchingExceptions {
            when (localMedia.info.mimeType) {
                MimeTypes.Apk -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (PermissionChecker.checkPermission(
                                context,
                                Manifest.permission.REQUEST_INSTALL_PACKAGES,
                                -1,
                                -1,
                                context.packageName
                            ) == PermissionChecker.PERMISSION_GRANTED &&
                            activityContext?.packageManager?.canRequestPackageInstalls() == false) {
                            pendingMedia = localMedia
                            activityContext?.startInstallFromSourceIntent(apkInstallLauncher!!).let { }
                        } else {
                            openFile(localMedia)
                        }
                    } else {
                        openFile(localMedia)
                    }
                }
                else -> openFile(localMedia)
            }
        }.onSuccess {
            Timber.v("Open media succeed")
        }.onFailure {
            Timber.e(it, "Open media failed")
        }
    }

    private suspend fun openFile(localMedia: LocalMedia) {
        val openMediaIntent = Intent(Intent.ACTION_VIEW)
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(localMedia.toShareableUri(), localMedia.info.mimeType)
        withContext(coroutineDispatchers.main) {
            activityContext?.startActivity(openMediaIntent)
        }
    }

    private fun LocalMedia.toShareableUri(): Uri {
        val mediaAsFile = this.toFile()
        val authority = "${buildMeta.applicationId}.fileprovider"
        return FileProvider.getUriForFile(context, authority, mediaAsFile).normalizeScheme()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveOnDiskUsingMediaStore(localMedia: LocalMedia) {
        val resolver = context.contentResolver
        val uniqueFilename = localMedia.uniqueDownloadFilename(resolver)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFilename)
            put(MediaStore.MediaColumns.MIME_TYPE, localMedia.info.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: error("MediaStore insert returned null — check DISPLAY_NAME=${localMedia.safeFilename()} MIME=${localMedia.info.mimeType}")
        try {
            localMedia.openStream().use { input ->
                resolver.openOutputStream(outputUri, "w").use { output ->
                    if (output == null) {
                        resolver.delete(outputUri, null, null)
                        error("MediaStore openOutputStream returned null for $outputUri")
                    }
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            // Mark as complete
            ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }.also { cv ->
                resolver.update(outputUri, cv, null, null)
            }
        } catch (e: Exception) {
            // Clean up the pending entry on failure
            try { resolver.delete(outputUri, null, null) } catch (_: Exception) {}
            throw e
        }
    }

    private fun saveOnDiskUsingExternalStorageApi(localMedia: LocalMedia) {
        val target = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            localMedia.safeFilename()
        )
        localMedia.openStream().use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun LocalMedia.openStream(): InputStream {
        // Try ContentResolver first (works for content://, may work for file:// on some devices)
        context.contentResolver.openInputStream(uri)?.let { return it }

        // Fall back to direct file access for file:// URIs
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val file = uri.toFile()
            if (file.exists() && file.canRead()) {
                return file.inputStream()
            }
            error("File not found or not readable: ${file.absolutePath}")
        }

        error("Unable to open input stream for $uri (scheme=${uri.scheme})")
    }

    private fun LocalMedia.uniqueDownloadFilename(resolver: ContentResolver): String {
        val baseName = safeFilename()
        // Split into name and extension
        val dotIndex = baseName.lastIndexOf('.')
        val stem = if (dotIndex > 0) baseName.substring(0, dotIndex) else baseName
        val ext = if (dotIndex > 0) baseName.substring(dotIndex) else ""

        // Query existing filenames in Downloads with the same stem
        val existingNames = mutableSetOf<String>()
        @Suppress("DEPRECATION")
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(Environment.DIRECTORY_DOWNLOADS, "$stem%$ext"),
            null,
        )?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                existingNames.add(cursor.getString(col))
            }
        }

        // If the original name is unique, use it
        if (baseName !in existingNames) return baseName

        // Generate a unique name: "image (1).jpg", "image (2).jpg", ...
        var counter = 1
        while (true) {
            val candidate = "$stem ($counter)$ext"
            if (candidate !in existingNames) return candidate
            counter++
            if (counter > 999) {
                // Fallback: use timestamp
                return "$stem-${System.currentTimeMillis()}$ext"
            }
        }
    }

    private fun LocalMedia.safeFilename(): String {
        // Use the original filename if available and non-blank
        val rawName = File(info.filename).name
        if (rawName.isNotBlank()) return rawName

        // Fall back to the URI's filename
        val uriName = uri.toFile().name
        if (uriName.isNotBlank()) return uriName

        // Last resort: generate one with the right extension from the MIME type
        val extension = info.mimeType
            ?.substringAfterLast('/')
            ?.takeUnless { it == "*" || it == "octet-stream" || it.isBlank() }
        return if (extension != null) "download.$extension" else "download"
    }

    /**
     * Tries to extract a file from the uri.
     */
    private fun LocalMedia.toFile(): File {
        return uri.toFile()
    }
}
