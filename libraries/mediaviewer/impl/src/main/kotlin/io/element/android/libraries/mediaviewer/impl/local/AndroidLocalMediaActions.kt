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
import io.element.android.libraries.androidutils.file.saveWithUniqueFileName
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
import java.io.IOException
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
            // Make a copy of the shared file in the cache directory, otherwise the original file will be gone once this screen is dismissed
            // and will prevent sharing the media to another room inside the app.
            val copiedFile = localMedia.uri.toFile()
                .copyTo(File(context.cacheDir, "temp/media/" + (localMedia.uri.lastPathSegment ?: "shared_file")), true)
            val shareableUri = copiedFile.toShareableUri()
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

    private fun File.toShareableUri(): Uri {
        val authority = "${buildMeta.applicationId}.fileprovider"
        return FileProvider.getUriForFile(context, authority, this).normalizeScheme()
    }

    private fun LocalMedia.toShareableUri(): Uri {
        return this.toFile().toShareableUri()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveOnDiskUsingMediaStore(localMedia: LocalMedia) {
        val resolver = context.contentResolver
        val (outputUri, _) = localMedia.insertWithUniqueName(resolver)
            ?: error("MediaStore insert failed after retries — base name=${localMedia.safeFilename()} MIME=${localMedia.info.mimeType}")
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

    /**
     * Attempts to insert a MediaStore entry with a unique DISPLAY_NAME.
     *
     * Unlike a pre-query approach (which has a TOCTOU race between checking
     * existing names and inserting), this uses the insert result itself as
     * the collision detector.  When MediaStore rejects the insert because
     * another file already has the chosen name, it returns null — we catch
     * that and retry with an incremented suffix.
     *
     * @return Pair of (outputUri, actuallyUsedFilename), or null if all
     *         attempts were exhausted.
     */
    private fun LocalMedia.insertWithUniqueName(resolver: ContentResolver): Pair<Uri, String>? {
        val baseName = safeFilename()
        val dotIndex = baseName.lastIndexOf('.')
        val stem = if (dotIndex > 0) baseName.substring(0, dotIndex) else baseName
        val ext = if (dotIndex > 0) baseName.substring(dotIndex) else ""
        val mimeType = info.mimeType

        // Candidate names to try in order: base, "stem (1).ext", "stem (2).ext", …
        // Any insert returning non-null wins immediately.
        var counter = 0
        while (true) {
            val candidate = if (counter == 0) baseName else "$stem ($counter)$ext"
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, candidate)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            @Suppress("DEPRECATION")
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            if (uri != null) return uri to candidate

            // Name collision — try next suffix
            counter++
            if (counter > 999) {
                // Final fallback: timestamp-based unique name
                val tsName = "$stem-${System.currentTimeMillis()}$ext"
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, tsName)
                val tsUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                return if (tsUri != null) tsUri to tsName else null
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
