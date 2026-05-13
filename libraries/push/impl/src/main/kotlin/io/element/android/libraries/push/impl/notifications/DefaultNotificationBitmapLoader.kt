/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.push.impl.notifications

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import androidx.core.graphics.drawable.IconCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.toBitmap
import coil3.transform.CircleCropTransformation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.AVATAR_THUMBNAIL_SIZE_IN_PIXEL
import io.element.android.libraries.matrix.ui.media.InitialsAvatarBitmapGenerator
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.push.api.notifications.NotificationBitmapLoader
import io.element.android.services.toolbox.api.sdk.BuildVersionSdkIntProvider
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@ContributesBinding(AppScope::class)
class DefaultNotificationBitmapLoader(
    @ApplicationContext private val context: Context,
    private val sdkIntProvider: BuildVersionSdkIntProvider,
    private val initialsAvatarBitmapGenerator: InitialsAvatarBitmapGenerator,
) : NotificationBitmapLoader {
    override suspend fun getRoomBitmap(
        avatarData: AvatarData,
        imageLoader: ImageLoader,
        targetSize: Long,
    ): Bitmap? {
        return try {
            loadBitmap(
                avatarData = avatarData,
                imageLoader = imageLoader,
                targetSize = targetSize,
            )
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load room bitmap")
            null
        }
    }

    override suspend fun getUserIcon(
        avatarData: AvatarData,
        imageLoader: ImageLoader,
    ): IconCompat? {
        if (sdkIntProvider.get() < Build.VERSION_CODES.P) {
            return null
        }
        return try {
            loadBitmap(
                avatarData = avatarData,
                imageLoader = imageLoader,
                targetSize = AVATAR_THUMBNAIL_SIZE_IN_PIXEL,
            )
                ?.let { IconCompat.createWithBitmap(it) }
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load user bitmap")
            null
        }
    }

    override suspend fun getClusterBitmap(
        heroes: List<AvatarData>,
        imageLoader: ImageLoader,
        targetSize: Long,
    ): Bitmap? {
        return try {
            createClusterBitmap(
                heroes = heroes,
                imageLoader = imageLoader,
                targetSize = targetSize,
            )
        } catch (e: Throwable) {
            Timber.e(e, "Unable to load room heroes bitmap")
            null
        }
    }

    private fun isDarkTheme(): Boolean {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private suspend fun loadBitmap(
        avatarData: AvatarData,
        imageLoader: ImageLoader,
        targetSize: Long
    ): Bitmap? {
        val path = avatarData.url
        val data = if (path != null) {
            MediaRequestData(
                source = MediaSource(path),
                kind = MediaRequestData.Kind.Thumbnail(targetSize),
            )
        } else {
            initialsAvatarBitmapGenerator.generateBitmap(
                size = targetSize.toInt(),
                avatarData = avatarData,
                useDarkTheme = isDarkTheme(),
            )
        }
        val imageRequest = ImageRequest.Builder(context)
            .data(data)
            .transformations(CircleCropTransformation())
            .build()
        return imageLoader.execute(imageRequest).image?.toBitmap()
    }

    private suspend fun createClusterBitmap(
        heroes: List<AvatarData>,
        imageLoader: ImageLoader,
        targetSize: Long,
    ): Bitmap? {
        val numberOfAvatars = min(heroes.size, 4)
        if (numberOfAvatars == 0) return null

        val size = targetSize.toInt()
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val offsetRadius = when (numberOfAvatars) {
            2 -> size / 4.2f
            3 -> size / 4.0f
            else -> size / 3.1f
        }
        val heroAvatarSize = when (numberOfAvatars) {
            2 -> size / 2.2f
            3 -> size / 2.4f
            else -> size / 2.2f
        }.toInt()
        val angleOffset = when (numberOfAvatars) {
            2 -> PI.toFloat()
            3 -> (7 * PI / 6).toFloat()
            else -> (13 * PI / 4).toFloat()
        }
        val angle = (2 * PI / numberOfAvatars).toFloat()
        val orderedHeroes = heroes.take(numberOfAvatars).let { avatars ->
            if (numberOfAvatars == 4) {
                listOf(avatars[0], avatars[1], avatars[3], avatars[2])
            } else {
                avatars
            }
        }

        orderedHeroes.forEachIndexed { index, hero ->
            val childBitmap = loadBitmap(
                avatarData = hero,
                imageLoader = imageLoader,
                targetSize = heroAvatarSize.toLong(),
            ) ?: return@forEachIndexed

            val scaledBitmap = Bitmap.createScaledBitmap(childBitmap, heroAvatarSize, heroAvatarSize, true)
            val centerX = size / 2f + offsetRadius * cos(angle * index + angleOffset)
            val centerY = size / 2f + offsetRadius * sin(angle * index + angleOffset)
            drawCircleBitmap(
                canvas = canvas,
                bitmap = scaledBitmap,
                left = centerX - heroAvatarSize / 2f,
                top = centerY - heroAvatarSize / 2f,
                size = heroAvatarSize.toFloat(),
            )
        }

        return result
    }

    private fun drawCircleBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        size: Float,
    ) {
        val checkpoint = canvas.save()
        val path = Path().apply {
            addCircle(left + size / 2f, top + size / 2f, size / 2f, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, left, top, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restoreToCount(checkpoint)
    }

}
