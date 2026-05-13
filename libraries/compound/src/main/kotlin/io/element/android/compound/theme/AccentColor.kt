/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * This file is part of the fork: rusty4444/element-x-android
 *
 * Accent color system for customizable app themes.
 */

package io.element.android.compound.theme

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
    val subtleDark: Color,
) {
    Default(
        displayName = "Default",
        primary = Color(0xFF4B9CE5),
        primaryHover = Color(0xFF3A8AC7),
        primaryPressed = Color(0xFF2D75A9),
        primaryAlpha = Color(0xCC4B9CE5),
        onPrimary = Color.White,
        subtle = Color(0xFFE5EFFA),
        subtleDark = Color(0xFF1A3658),
    ),
    Orange(
        displayName = "Orange",
        primary = Color(0xFFFF8C00),
        primaryHover = Color(0xFFE07300),
        primaryPressed = Color(0xFFB35700),
        primaryAlpha = Color(0xCCFF8C00),
        onPrimary = Color.Black,
        subtle = Color(0xFFFFF0D6),
        subtleDark = Color(0xFF3D2600),
    ),
    Purple(
        displayName = "Purple",
        primary = Color(0xFF9C59D1),
        primaryHover = Color(0xFF823CBA),
        primaryPressed = Color(0xFF6A2AA8),
        primaryAlpha = Color(0xCC9C59D1),
        onPrimary = Color.White,
        subtle = Color(0xFFF0E4FD),
        subtleDark = Color(0xFF2D1B48),
    ),
    Blue(
        displayName = "Blue",
        primary = Color(0xFF3B82F6),
        primaryHover = Color(0xFF2563EB),
        primaryPressed = Color(0xFF1D4ED8),
        primaryAlpha = Color(0xCC3B82F6),
        onPrimary = Color.White,
        subtle = Color(0xFFDBEAFE),
        subtleDark = Color(0xFF172D5E),
    ),
    Teal(
        displayName = "Teal",
        primary = Color(0xFF0D9488),
        primaryHover = Color(0xFF0F766E),
        primaryPressed = Color(0xFF115E59),
        primaryAlpha = Color(0xCC0D9488),
        onPrimary = Color.White,
        subtle = Color(0xFFCCFBF1),
        subtleDark = Color(0xFF042F2E),
    ),
    Pink(
        displayName = "Pink",
        primary = Color(0xFFE91E63),
        primaryHover = Color(0xFFC2185B),
        primaryPressed = Color(0xFFAD1457),
        primaryAlpha = Color(0xCCE91E63),
        onPrimary = Color.White,
        subtle = Color(0xFFFCE4EC),
        subtleDark = Color(0xFF3F0A1E),
    ),
    Red(
        displayName = "Red",
        primary = Color(0xFFEF4444),
        primaryHover = Color(0xFFDC2626),
        primaryPressed = Color(0xFFB91C1C),
        primaryAlpha = Color(0xCCDC2626),
        onPrimary = Color.White,
        subtle = Color(0xFFFEE2E2),
        subtleDark = Color(0xFF450A0A),
    ),
    Green(
        displayName = "Green",
        primary = Color(0xFF22C55E),
        primaryHover = Color(0xFF16A34A),
        primaryPressed = Color(0xFF15803D),
        primaryAlpha = Color(0xCC22C55E),
        onPrimary = Color.Black,
        subtle = Color(0xFFDCFCE7),
        subtleDark = Color(0xFF052E16),
    ),
    Indigo(
        displayName = "Indigo",
        primary = Color(0xFF6366F1),
        primaryHover = Color(0xFF4F46E5),
        primaryPressed = Color(0xFF4338CA),
        primaryAlpha = Color(0xCC6366F1),
        onPrimary = Color.White,
        subtle = Color(0xFFE0E1FA),
        subtleDark = Color(0xFF1E1B4B),
    ),
    Amber(
        displayName = "Amber",
        primary = Color(0xFFF59E0B),
        primaryHover = Color(0xFFD97706),
        primaryPressed = Color(0xFFB45309),
        primaryAlpha = Color(0xCCF59E0B),
        onPrimary = Color.Black,
        subtle = Color(0xFFFFF7ED),
        subtleDark = Color(0xFF431C00),
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
 *
 * @param isDark Whether the dark theme variant is being applied. This controls
 *   which subtle colors are used.
 */
fun SemanticColors.withAccentColor(accent: AccentColor, isDark: Boolean = false): SemanticColors {
    val subtle = if (isDark) accent.subtleDark else accent.subtle
    return copy(
        // Background accent
        bgAccentHovered = accent.primaryHover,
        bgAccentPressed = accent.primaryPressed,
        bgAccentRest = accent.primary,
        bgAccentSelected = accent.primary,
        // Badge - unread count bubbles, notification badges
        bgBadgeAccent = accent.primary,
        textBadgeAccent = accent.onPrimary,
        // Subtle surfaces for chat bubbles with accent tint
        bgSubtlePrimary = subtle,
        bgSubtleSecondary = subtle.copy(alpha = 0.85f),
        bgSubtleSecondaryLevel0 = subtle.copy(alpha = 0.65f),
        // Gradient tokens for message bubbles and bloom effects
        gradientSubtleStop1 = accent.primaryAlpha,
        gradientSubtleStop2 = subtle,
        gradientSubtleStop3 = accent.primary.copy(alpha = 0.3f),
        gradientSubtleStop4 = accent.primary.copy(alpha = 0.15f),
        gradientSubtleStop5 = subtle.copy(alpha = 0.8f),
        gradientSubtleStop6 = accent.primary.copy(alpha = 0.2f),
        // Primary action backgrounds
        bgActionPrimaryDisabled = Color.Gray.copy(alpha = 0.5f),
        bgActionPrimaryHovered = accent.primaryHover,
        bgActionPrimaryPressed = accent.primaryPressed,
        bgActionPrimaryRest = accent.primary,
        bgActionSecondaryHovered = accent.primaryAlpha,
        bgActionSecondaryPressed = accent.primary.copy(alpha = 0.5f),
        bgActionSecondaryRest = subtle,
        // Border colors
        borderAccentPrimary = accent.primary,
        borderAccentSubtle = subtle,
        // Icon and text accent colors
        iconAccentPrimary = accent.primary,
        iconAccentTertiary = accent.primary.copy(alpha = 0.6f),
        textActionAccent = accent.primary,
    )
}
