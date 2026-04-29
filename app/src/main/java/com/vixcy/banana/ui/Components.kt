package com.vixcy.banana.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider

// ─────────────────────────────────────────────────────────────────────────────
// Pressable — the universal press-feedback wrapper.
// Wrap any tap-target with this to get an iOS-style scale-down + spring back.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressable_scale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) {
                // Light tactile feedback on every tap — matches iOS impact .light
                if (haptic && enabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer — animated skeleton loader.
// Usage: Box(Modifier.shimmer().background(...).size(...))
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val density = LocalDensity.current
    return this.then(
        Modifier.graphicsLayer {  /* no-op layer for compose layout cohesion */ }
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEEEDE7),
                        Color(0xFFF7F5F0),
                        Color(0xFFEEEDE7)
                    ),
                    start = Offset(translate * 600f, 0f),
                    end = Offset((translate + 1f) * 600f, 0f)
                )
            )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// BananaField — custom text input. No Material default look. Soft focus glow.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BananaField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = Banana.Type.bodyMedium.copy(color = Banana.Color.Ink),
    placeholderStyle: TextStyle = Banana.Type.body.copy(color = Banana.Color.InkMuted),
    singleLine: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "field_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (focused) Banana.Color.Surface else Banana.Color.SurfaceDim,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "field_bg"
    )

    val selectionColors = TextSelectionColors(
        handleColor = Banana.Color.Accent,
        backgroundColor = Banana.Color.Accent.copy(alpha = 0.25f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(Banana.Radius.m))
                .background(bgColor)
                .border(
                    width = 1.5.dp,
                    color = Banana.Color.Accent.copy(alpha = borderAlpha * 0.55f),
                    shape = RoundedCornerShape(Banana.Radius.m)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(10.dp))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        ProvideTextStyle(placeholderStyle) {
                            Text(placeholder)
                        }
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = textStyle,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        singleLine = singleLine,
                        cursorBrush = SolidColor(Banana.Color.Accent),
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(10.dp))
                    trailing()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AccentChip — pill-shaped chip with text. Tinted variant.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AccentChip(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = Banana.Color.AccentSoft,
    fg: Color = Banana.Color.Accent,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 5.dp,
    style: TextStyle = Banana.Type.captionBold
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Banana.Radius.pill))
            .background(bg)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Text(text, style = style, color = fg)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IconButtonSoft — circular icon button with press scale.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IconButtonSoft(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Banana.Color.SurfaceDim,
    size: Dp = 36.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Pressable(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(Banana.Radius.pill))
                .background(bg),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
