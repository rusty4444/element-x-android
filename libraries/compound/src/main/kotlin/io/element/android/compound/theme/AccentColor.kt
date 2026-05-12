/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * This file is part of the fork: rusty4444/element-x-android
 *
 * Accent color system for customizable app themes.
 */

package io.element.android.compound.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.element.android.compound.tokens.generated.SemanticColors

/**
 * Custom accent colors that users can choose for the app theme.
 * Each color overrides the primary/accent tokens on top of the base Compound theme.
 */
enum class AccentColor(
    val displayName: String,
    val primary: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
    val primaryAlpha: Color,
    val onPrimary: Color,
    val subtle: Color,
) {
    Default(
        displayName = "Default",
        primary = Color(0xFF4B9CE5),
        primaryHover = Color(0xFF3A8AC7),
        primaryPressed = Color(0xFF2D75A9),
        primaryAlpha = Color(0xCC4B9CE5),
        onPrimary = Color.White,
        subtle = Color(0xFFE5EFFA),
    ),
    Orange(
        displayName = "Orange",
        primary = Color(0xFFFF8C00),
        primaryHover = Color(0xFFE07300),
        primaryPressed = Color(0xFFB35700),
        primaryAlpha = Color(0xCCFF8C00),
        onPrimary = Color.Black,
        subtle = Color(0xFFFFF0D6),
    ),
    Purple(
        displayName = "Purple",
        primary = Color(0xFF9C59D1),
        primaryHover = Color(0xFF823CBA),
        primaryPressed = Color(0xFF6A2AA8),
        primaryAlpha = Color(0xCC9C59D1),
        onPrimary = Color.White,
        subtle = Color(0xFFF0E4FD),
    ),
    Blue(
        displayName = "Blue",
        primary = Color(0xFF3B82F6),
        primaryHover = Color(0xFF2563EB),
        primaryPressed = Color(0xFF1D4ED8),
        primaryAlpha = Color(0xCC3B82F6),
        onPrimary = Color.White,
        subtle = Color(0xFFDBEAFE),
    ),
    Teal(
        displayName = "Teal",
        primary = Color(0xFF009688),
        primaryHover = Color(0xFF00796F),
        primaryPressed = Color(0xFF00635A),
        primaryAlpha = Color(0xCC009688),
        onPrimary = Color.White,
        subtle = Color(0xFFCCF0EE),
    ),
    Pink(
        displayName = "Pink",
        primary = Color(0xFFE91E63),
        primaryHover = Color(0xFFC2185B),
        primaryPressed = Color(0xFFAD1457),
        primaryAlpha = Color(0xCCC2185B),
        onPrimary = Color.White,
        subtle = Color(0xFFFCE4EC),
    ),
    Red(
        displayName = "Red",
        primary = Color(0xFFEF4444),
        primaryHover = Color(0xFFDC2626),
        primaryPressed = Color(0xFFB91C1C),
        primaryAlpha = Color(0xCCDC2626),
        onPrimary = Color.White,
        subtle = Color(0xFFFEE2E2),
    ),
    Green(
        displayName = "Green",
        primary = Color(0xFF22C55E),
        primaryHover = Color(0xFF16A34A),
        primaryPressed = Color(0xFF15803D),
        primaryAlpha = Color(0xCC22C55E),
        onPrimary = Color.Black,
        subtle = Color(0xFFDCFCE7),
    ),
    Indigo(
        displayName = "Indigo",
        primary = Color(0xFF6366F1),
        primaryHover = Color(0xFF4F46E5),
        primaryPressed = Color(0xFF4338CA),
        primaryAlpha = Color(0xCC4F46E5),
        onPrimary = Color.White,
        subtle = Color(0xFFE0E1FA),
    ),
    Amber(
        displayName = "Amber",
        primary = Color(0xFFF59E0B),
        primaryHover = Color(0xFFD97706),
        primaryPressed = Color(0xFFB45309),
        primaryAlpha = Color(0xCCF59E0B),
        onPrimary = Color.Black,
        subtle = Color(0xFFFDF4DD),
    );

    companion object {
        fun fromString(value: String?): AccentColor {
            if (value == null || value == Default.name) return Default
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: Default
        }

        val All = entries.toList()
    }
}

/**
 * Apply accent color overrides to the base [SemanticColors].
 * Returns a new SemanticColors instance with overridden accent tokens.
 */
fun SemanticColors.withAccentColor(accent: AccentColor): SemanticColors {
    if (accent == AccentColor.Default) return this
    return copy(
        // Background accent
        bgAccentRest = accent.primary,
        bgAccentSelected = accent.primary,
        // Primary action backgrounds
        bgActionPrimaryDisabled = Color.Gray.copy(alpha = 0.5f),
        bgActionPrimaryHovered = accent.primaryHover,
        bgActionPrimaryPressed = accent.primaryPressed,
        bgActionPrimaryRest = accent.primary,
        bgActionSecondaryHovered = accent.primaryAlpha,
        bgActionSecondaryPressed = accent.primary.copy(alpha = 0.5f),
        bgActionSecondaryRest = accent.subtle,
        // ACCENT/HOVERED
        iconAccentHovered = accent.primaryHover,
        iconAccentPressed = accent.primaryPressed,
        iconAccentSelected = accent.primary,
        // Border colors
        borderAccentPrimary = accent.primary,
        // Text accent colors
        iconAccentPrimary = accent.primary,
        textActionAccent = accent.primary,

    )
}
