/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.theme

import androidx.compose.ui.graphics.Color
import io.element.android.compound.annotations.CoreColorToken
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.compound.tokens.generated.internal.DarkColorTokens
import io.element.android.compound.tokens.generated.internal.LightColorTokens

/**
 * User-selectable accent colors for theming the app.
 * Each entry defines light and dark palettes that override the accent-related
 * [SemanticColors] tokens when applied via [withAccentColor].
 */
@OptIn(CoreColorToken::class)
enum class AccentColor {

    /** Default Element Green (compound brand). Uses existing token values — no override needed. */
    Green {
        override fun light() = AccentPalette(
            isLight = true,
            primary = LightColorTokens.colorGreen900,
            hovered = LightColorTokens.colorGreen1000,
            pressed = LightColorTokens.colorGreen1100,
            selectedBg = LightColorTokens.colorAlphaGreen300,
            tertiary = LightColorTokens.colorGreen800,
            subtleBorder = LightColorTokens.colorGreen700,
            gradient1 = LightColorTokens.colorGreen500,
            gradient2 = LightColorTokens.colorGreen700,
            gradient3 = LightColorTokens.colorGreen900,
            gradient4 = LightColorTokens.colorGreen1100,
            bgBadge = LightColorTokens.colorGreen400,
            textBadge = LightColorTokens.colorGreen1100,
        )

        override fun dark() = AccentPalette(
            isLight = false,
            primary = DarkColorTokens.colorGreen900,
            hovered = DarkColorTokens.colorGreen1000,
            pressed = DarkColorTokens.colorGreen1100,
            selectedBg = DarkColorTokens.colorAlphaGreen300,
            tertiary = DarkColorTokens.colorGreen800,
            subtleBorder = DarkColorTokens.colorGreen700,
            gradient1 = DarkColorTokens.colorGreen1100,
            gradient2 = DarkColorTokens.colorGreen900,
            gradient3 = DarkColorTokens.colorGreen700,
            gradient4 = DarkColorTokens.colorGreen500,
            bgBadge = DarkColorTokens.colorGreen400,
            textBadge = DarkColorTokens.colorGreen1100,
        )
    },

    /** Purple — Violet spectrum */
    Purple {
        override fun light() = AccentPalette(
            primary = Color(0xff7C3AED),
            hovered = Color(0xff6D28D9),
            pressed = Color(0xff5B21B6),
            selectedBg = Color(0x1c7C3AED),
            tertiary = Color(0xff8B5CF6),
            subtleBorder = Color(0xffA78BFA),
            gradient1 = Color(0xffC4B5FD),
            gradient2 = Color(0xffA78BFA),
            gradient3 = Color(0xff7C3AED),
            gradient4 = Color(0xff5B21B6),
            bgBadge = Color(0xffDDD6FE),
            textBadge = Color(0xff5B21B6),
        )

        override fun dark() = AccentPalette(
            primary = Color(0xffA78BFA),
            hovered = Color(0xffC4B5FD),
            pressed = Color(0xffDDD6FE),
            selectedBg = Color(0x1cA78BFA),
            tertiary = Color(0xff8B5CF6),
            subtleBorder = Color(0xff7C3AED),
            gradient1 = Color(0xffDDD6FE),
            gradient2 = Color(0xffC4B5FD),
            gradient3 = Color(0xffA78BFA),
            gradient4 = Color(0xff8B5CF6),
            bgBadge = Color(0xff6D28D9),
            textBadge = Color(0xffDDD6FE),
        )
    },

    /** Orange */
    Orange {
        override fun light() = AccentPalette(
            primary = Color(0xffEA580C),
            hovered = Color(0xffC2410C),
            pressed = Color(0xff9A3412),
            selectedBg = Color(0x1cEA580C),
            tertiary = Color(0xffF97316),
            subtleBorder = Color(0xffFB923C),
            gradient1 = Color(0xffFDBA74),
            gradient2 = Color(0xffFB923C),
            gradient3 = Color(0xffEA580C),
            gradient4 = Color(0xff9A3412),
            bgBadge = Color(0xffFED7AA),
            textBadge = Color(0xff9A3412),
        )

        override fun dark() = AccentPalette(
            primary = Color(0xffFB923C),
            hovered = Color(0xffFDBA74),
            pressed = Color(0xffFED7AA),
            selectedBg = Color(0x1cFB923C),
            tertiary = Color(0xffF97316),
            subtleBorder = Color(0xffEA580C),
            gradient1 = Color(0xffFED7AA),
            gradient2 = Color(0xffFDBA74),
            gradient3 = Color(0xffFB923C),
            gradient4 = Color(0xffF97316),
            bgBadge = Color(0xffC2410C),
            textBadge = Color(0xffFED7AA),
        )
    },

    /** Blue */
    Blue {
        override fun light() = AccentPalette(
            primary = Color(0xff2563EB),
            hovered = Color(0xff1D4ED8),
            pressed = Color(0xff1E40AF),
            selectedBg = Color(0x1c2563EB),
            tertiary = Color(0xff3B82F6),
            subtleBorder = Color(0xff60A5FA),
            gradient1 = Color(0xff93C5FD),
            gradient2 = Color(0xff60A5FA),
            gradient3 = Color(0xff2563EB),
            gradient4 = Color(0xff1E40AF),
            bgBadge = Color(0xffBFDBFE),
            textBadge = Color(0xff1E40AF),
        )

        override fun dark() = AccentPalette(
            primary = Color(0xff60A5FA),
            hovered = Color(0xff93C5FD),
            pressed = Color(0xffBFDBFE),
            selectedBg = Color(0x1c60A5FA),
            tertiary = Color(0xff3B82F6),
            subtleBorder = Color(0xff2563EB),
            gradient1 = Color(0xffBFDBFE),
            gradient2 = Color(0xff93C5FD),
            gradient3 = Color(0xff60A5FA),
            gradient4 = Color(0xff3B82F6),
            bgBadge = Color(0xff1D4ED8),
            textBadge = Color(0xffBFDBFE),
        )
    },

    /** Pink */
    Pink {
        override fun light() = AccentPalette(
            primary = Color(0xffDB2777),
            hovered = Color(0xffBE185D),
            pressed = Color(0xff9D174D),
            selectedBg = Color(0x1cDB2777),
            tertiary = Color(0xffEC4899),
            subtleBorder = Color(0xffF472B6),
            gradient1 = Color(0xffF9A8D4),
            gradient2 = Color(0xffF472B6),
            gradient3 = Color(0xffDB2777),
            gradient4 = Color(0xff9D174D),
            bgBadge = Color(0xffFBCFE8),
            textBadge = Color(0xff9D174D),
        )

        override fun dark() = AccentPalette(
            primary = Color(0xffF472B6),
            hovered = Color(0xffF9A8D4),
            pressed = Color(0xffFBCFE8),
            selectedBg = Color(0x1cF472B6),
            tertiary = Color(0xffEC4899),
            subtleBorder = Color(0xffDB2777),
            gradient1 = Color(0xffFBCFE8),
            gradient2 = Color(0xffF9A8D4),
            gradient3 = Color(0xffF472B6),
            gradient4 = Color(0xffEC4899),
            bgBadge = Color(0xffBE185D),
            textBadge = Color(0xffFBCFE8),
        )
    },

    /** Teal */
    Teal {
        override fun light() = AccentPalette(
            primary = Color(0xff0D9488),
            hovered = Color(0xff0F766E),
            pressed = Color(0xff115E59),
            selectedBg = Color(0x1c0D9488),
            tertiary = Color(0xff14B8A6),
            subtleBorder = Color(0xff2DD4BF),
            gradient1 = Color(0xff5EEAD4),
            gradient2 = Color(0xff2DD4BF),
            gradient3 = Color(0xff0D9488),
            gradient4 = Color(0xff115E59),
            bgBadge = Color(0xff99F6E4),
            textBadge = Color(0xff115E59),
        )

        override fun dark() = AccentPalette(
            primary = Color(0xff2DD4BF),
            hovered = Color(0xff5EEAD4),
            pressed = Color(0xff99F6E4),
            selectedBg = Color(0x1c2DD4BF),
            tertiary = Color(0xff14B8A6),
            subtleBorder = Color(0xff0D9488),
            gradient1 = Color(0xff99F6E4),
            gradient2 = Color(0xff5EEAD4),
            gradient3 = Color(0xff2DD4BF),
            gradient4 = Color(0xff14B8A6),
            bgBadge = Color(0xff0F766E),
            textBadge = Color(0xff99F6E4),
        )
    };

    abstract fun light(): AccentPalette
    abstract fun dark(): AccentPalette
}

/**
 * Holds the color overrides for a single theme variant (light or dark).
 * Used internally by [AccentColor] to define per-entry palettes.
 */
data class AccentPalette(
    val isLight: Boolean = true,
    /** Accent rest (primary) */
    val primary: Color,
    /** Accent hovered */
    val hovered: Color,
    /** Accent pressed */
    val pressed: Color,
    /** Accent selected background (semi-transparent) */
    val selectedBg: Color,
    /** Accent tertiary icon */
    val tertiary: Color,
    /** Accent subtle border */
    val subtleBorder: Color,
    /** Gradient stop 1 (lightest) */
    val gradient1: Color,
    /** Gradient stop 2 */
    val gradient2: Color,
    /** Gradient stop 3 */
    val gradient3: Color,
    /** Gradient stop 4 (darkest) */
    val gradient4: Color,
    /** Accent badge background */
    val bgBadge: Color,
    /** Accent badge text */
    val textBadge: Color,
)

/**
 * Derive alpha gradient stops from the main palette colors.
 * Uses the same semi-transparent pattern as the compound alpha green tokens.
 */
private fun AccentPalette.alphaGradients(): List<Color> = listOf(
    selectedBg,                                                            // ~500 alpha
    primary.copy(alpha = 0.10f),   // ~400
    primary.copy(alpha = 0.07f),   // ~300
    primary.copy(alpha = 0.04f),   // ~200
    primary.copy(alpha = 0.02f),   // ~100
    Color.Transparent,                                                     // stop 6
)

/**
 * Applies this accent color to the given [SemanticColors], returning a new copy
 * with all accent-related tokens overridden.
 *
 * For [AccentColor.Green] (the default), the original colors are returned unchanged.
 */
fun SemanticColors.withAccentColor(color: AccentColor): SemanticColors {
    if (color == AccentColor.Green) return this

    val palette = if (isLight) color.light() else color.dark()
    val alphas = palette.alphaGradients()

    return copy(
        bgAccentRest = palette.primary,
        bgAccentHovered = palette.hovered,
        bgAccentPressed = palette.pressed,
        bgAccentSelected = palette.selectedBg,
        iconAccentPrimary = palette.primary,
        iconAccentTertiary = palette.tertiary,
        textActionAccent = palette.primary,
        borderAccentPrimary = palette.primary,
        borderAccentSubtle = palette.subtleBorder,
        gradientActionStop1 = palette.gradient1,
        gradientActionStop2 = palette.gradient2,
        gradientActionStop3 = palette.gradient3,
        gradientActionStop4 = palette.gradient4,
        gradientSubtleStop1 = alphas[0],
        gradientSubtleStop2 = alphas[1],
        gradientSubtleStop3 = alphas[2],
        gradientSubtleStop4 = alphas[3],
        gradientSubtleStop5 = alphas[4],
        gradientSubtleStop6 = alphas[5],
        bgBadgeAccent = palette.bgBadge,
        textBadgeAccent = palette.textBadge,
    )
}
