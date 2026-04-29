package com.vixcy.banana.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Banana design system — single source of truth for color, type, motion, spacing.
 * Every screen and component pulls tokens from here. Never hard-code values elsewhere.
 */
object Banana {

    // ──────────────────────────── Color ────────────────────────────
    object Color {
        val Bg            = androidx.compose.ui.graphics.Color(0xFFF5F3EE)
        val BgElevated    = androidx.compose.ui.graphics.Color(0xFFFAF8F4)
        val Surface       = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val SurfaceDim    = androidx.compose.ui.graphics.Color(0xFFF7F5F0)

        val Ink           = androidx.compose.ui.graphics.Color(0xFF0A0A0A)
        val InkSoft       = androidx.compose.ui.graphics.Color(0xFF1F1F1F)
        val InkSecondary  = androidx.compose.ui.graphics.Color(0xFF6B6B6B)
        val InkMuted      = androidx.compose.ui.graphics.Color(0xFFA3A3A0)
        val InkSubtle     = androidx.compose.ui.graphics.Color(0xFFD4D3CF)

        val Stroke        = androidx.compose.ui.graphics.Color(0xFFECEAE4)
        val StrokeSoft    = androidx.compose.ui.graphics.Color(0xFFF3F1EC)

        val Accent        = androidx.compose.ui.graphics.Color(0xFFE85D2F)   // brand banana-orange
        val AccentDeep    = androidx.compose.ui.graphics.Color(0xFFC74A23)
        val AccentSoft    = androidx.compose.ui.graphics.Color(0xFFFFEFEA)
        val AccentSofter  = androidx.compose.ui.graphics.Color(0xFFFFF7F4)

        val Mint          = androidx.compose.ui.graphics.Color(0xFF22A07A)
        val MintSoft      = androidx.compose.ui.graphics.Color(0xFFE0F4ED)

        val Lavender      = androidx.compose.ui.graphics.Color(0xFF7B7AD9)
        val LavenderSoft  = androidx.compose.ui.graphics.Color(0xFFEEEDFC)

        val Sky           = androidx.compose.ui.graphics.Color(0xFF378ADD)
        val SkySoft       = androidx.compose.ui.graphics.Color(0xFFE5EFFB)

        val Sand          = androidx.compose.ui.graphics.Color(0xFFB39572)
        val SandSoft      = androidx.compose.ui.graphics.Color(0xFFF2EBE0)

        val Error         = androidx.compose.ui.graphics.Color(0xFFD93B3B)
        val ErrorSoft     = androidx.compose.ui.graphics.Color(0xFFFCEBEB)

        val ScrimSoft     = androidx.compose.ui.graphics.Color(0x14000000)
    }

    // ──────────────────────────── Typography ────────────────────────────
    // Tight letter-spacing on display sizes, looser on caps labels — Apple SF style.
    object Type {
        val display      = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-2.0).sp)
        val largeTitle   = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-1.0).sp)
        val title        = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-0.5).sp)
        val title2       = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
        val headline     = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
        val body         = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal,   letterSpacing = (-0.1).sp)
        val bodyMedium   = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,   letterSpacing = (-0.1).sp)
        val callout      = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,   letterSpacing = (-0.1).sp)
        val footnote     = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
        val captionBold  = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
        val tagLabel     = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    }

    // ──────────────────────────── Spacing (4-pt grid) ────────────────────────────
    object Space {
        val xxs = 2.dp
        val xs  = 4.dp
        val s   = 8.dp
        val m   = 12.dp
        val l   = 16.dp
        val xl  = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
        val huge = 48.dp
    }

    // ──────────────────────────── Radii ────────────────────────────
    object Radius {
        val xs   = 8.dp
        val s    = 12.dp
        val m    = 16.dp
        val l    = 20.dp
        val xl   = 24.dp
        val xxl  = 28.dp
        val pill = 999.dp
    }

    // ──────────────────────────── Motion ────────────────────────────
    object Motion {
        // Easing curves (iOS-style — start fast, settle slowly)
        val EaseOut       = CubicBezierEasing(0.22f, 1.00f, 0.36f, 1.00f)   // ease-out-quint
        val EaseOutSoft   = CubicBezierEasing(0.16f, 1.00f, 0.30f, 1.00f)   // ease-out-expo
        val EaseInOut     = CubicBezierEasing(0.83f, 0.00f, 0.17f, 1.00f)   // ease-in-out-quint
        val EaseInOutSoft = CubicBezierEasing(0.65f, 0.00f, 0.35f, 1.00f)   // ease-in-out-cubic

        // Spring presets (use these instead of raw spring() everywhere)
        val SpringSoft    = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        val SpringSnappy  = spring<Float>(dampingRatio = 0.90f, stiffness = Spring.StiffnessMedium)
        val SpringBouncy  = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
        val SpringGentle  = spring<Float>(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow)

        // Standard durations
        const val Tap     = 90    // press → release
        const val Quick   = 180   // small UI changes
        const val Smooth  = 320   // standard transitions
        const val Slow    = 520   // hero / large content
    }
}

// ──────────────────────────── Shadows ────────────────────────────
/**
 * Apple-style soft layered shadow. Two-tone (ambient + spot) with the spot
 * intentionally weighted toward warm-black — so cards look elevated against
 * the warm beige bg without ever feeling floaty.
 *
 * Use BEFORE clip+background in a modifier chain.
 */
fun Modifier.bananaShadow(
    elevation: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(Banana.Radius.xl),
    ambient: Color = Color(0x0A000000),
    spot: Color = Color(0x18000000)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ambient,
    spotColor = spot
)

/** Tighter, hairline shadow for small chips / buttons. */
fun Modifier.bananaShadowSm(
    shape: Shape = RoundedCornerShape(Banana.Radius.m)
): Modifier = this.bananaShadow(
    elevation = 6.dp,
    shape = shape,
    spot = Color(0x12000000)
)

/** Soft, deeper shadow for the FAB / floating elements. */
fun Modifier.bananaShadowFab(
    shape: Shape = RoundedCornerShape(Banana.Radius.l)
): Modifier = this.bananaShadow(
    elevation = 18.dp,
    shape = shape,
    ambient = Color(0x14000000),
    spot = Color(0x26000000)
)

