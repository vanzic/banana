package com.vixcy.banana

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vixcy.banana.ui.AccentChip
import com.vixcy.banana.ui.Banana
import com.vixcy.banana.ui.BananaField
import com.vixcy.banana.ui.Pressable
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Hero entrance — slides up from below + fades
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Banana.Color.BgElevated,
                        Banana.Color.Bg,
                        Banana.Color.SurfaceDim
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = Banana.Space.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── Floating hero banana ─────────────────────────────────────
            FloatingBanana(visible = entered)

            Spacer(Modifier.height(20.dp))

            // ── Title ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(420, delayMillis = 80, easing = Banana.Motion.EaseOut)) +
                        slideInVertically(tween(540, delayMillis = 80, easing = Banana.Motion.EaseOut)) { it / 4 }
            ) {
                Text(
                    "banana",
                    style = Banana.Type.largeTitle.copy(
                        fontSize = 38.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = Banana.Color.Ink
                )
            }

            Spacer(Modifier.height(6.dp))

            AnimatedContent(
                targetState = isLogin,
                transitionSpec = {
                    (fadeIn(tween(220, easing = Banana.Motion.EaseOut)) +
                            slideInVertically(tween(260, easing = Banana.Motion.EaseOut)) { it / 6 })
                        .togetherWith(
                            fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 6 }
                        )
                },
                label = "tagline"
            ) { login ->
                Text(
                    if (login) "Your wallet, on autopilot." else "Join the cleanest expense tracker.",
                    style = Banana.Type.body,
                    color = Banana.Color.InkSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Mode segmented toggle ────────────────────────────────────
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(380, delayMillis = 200, easing = Banana.Motion.EaseOut)) +
                        slideInVertically(tween(480, delayMillis = 200, easing = Banana.Motion.EaseOut)) { it / 5 }
            ) {
                ModeToggle(
                    isLogin = isLogin,
                    onModeChange = { isLogin = it; errorMsg = null }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Form fields ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(420, delayMillis = 280, easing = Banana.Motion.EaseOut)) +
                        slideInVertically(tween(520, delayMillis = 280, easing = Banana.Motion.EaseOut)) { it / 6 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BananaField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        placeholder = "Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    BananaField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        placeholder = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error message — slides in + spring
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn(tween(180)) +
                                expandVertically(animationSpec = spring(0.7f, Spring.StiffnessMedium)) +
                                slideInVertically(tween(220, easing = Banana.Motion.EaseOut)) { it / 2 },
                        exit = fadeOut(tween(140)) + shrinkVertically(animationSpec = tween(160))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Banana.Radius.s))
                                .background(Banana.Color.ErrorSoft)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("!", color = Banana.Color.Error, style = Banana.Type.captionBold,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(Banana.Radius.pill))
                                    .background(Color.White),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                errorMsg ?: "",
                                color = Banana.Color.Error,
                                style = Banana.Type.footnote.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Primary CTA ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(440, delayMillis = 360, easing = Banana.Motion.EaseOut)) +
                        slideInVertically(tween(560, delayMillis = 360, easing = Banana.Motion.EaseOut)) { it / 6 }
            ) {
                PrimaryCta(
                    text = if (isLogin) "Sign in" else "Create account",
                    loading = isLoading,
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "Please fill in all fields"
                            return@PrimaryCta
                        }
                        isLoading = true
                        if (isLogin) {
                            AuthManager.signIn(email, password) { success, error ->
                                isLoading = false
                                if (success) onAuthSuccess()
                                else errorMsg = error ?: "Login failed"
                            }
                        } else {
                            AuthManager.signUp(email, password) { success, error ->
                                isLoading = false
                                if (success) onAuthSuccess()
                                else errorMsg = error ?: "Signup failed"
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Footer ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(400, delayMillis = 480))
            ) {
                Text(
                    "By continuing, you agree to Banana's terms.",
                    style = Banana.Type.footnote,
                    color = Banana.Color.InkMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Floating banana hero — gentle infinite vertical drift + tilt
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun FloatingBanana(visible: Boolean) {
    val transition = rememberInfiniteTransition(label = "banana_float")
    val y by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )
    val rot by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot"
    )

    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(0.55f, Spring.StiffnessMediumLow),
        label = "entrance"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(420, easing = Banana.Motion.EaseOut),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                translationY = y
                rotationZ = rot
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft glow underneath
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(Banana.Radius.pill))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Banana.Color.AccentSoft,
                            Banana.Color.AccentSoft.copy(alpha = 0f)
                        )
                    )
                )
        )
        Text("🍌", fontSize = 72.sp)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ModeToggle — segmented pill switch (Sign in / Sign up). Active pill slides.
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun ModeToggle(isLogin: Boolean, onModeChange: (Boolean) -> Unit) {
    val targetOffset by animateFloatAsState(
        targetValue = if (isLogin) 0f else 1f,
        animationSpec = spring(0.75f, Spring.StiffnessMediumLow),
        label = "toggle_offset"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Banana.Radius.pill))
            .background(Banana.Color.SurfaceDim)
            .padding(4.dp)
    ) {
        BoxWithConstraints {
            val pillWidth = maxWidth / 2
            // Sliding active pill
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(38.dp)
                    .graphicsLayer { translationX = targetOffset * pillWidth.toPx() }
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(Banana.Color.Surface)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ToggleLabel(
                    text = "Sign in",
                    selected = isLogin,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(true) }
                )
                ToggleLabel(
                    text = "Sign up",
                    selected = !isLogin,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(false) }
                )
            }
        }
    }
}

@Composable
private fun ToggleLabel(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (selected) Banana.Color.Ink else Banana.Color.InkMuted,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "label_color"
    )
    Pressable(onClick = onClick, modifier = modifier, pressedScale = 0.97f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PrimaryCta — large rounded action button with loading state
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun PrimaryCta(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) Banana.Color.Accent else Banana.Color.Accent.copy(alpha = 0.45f),
        animationSpec = tween(220),
        label = "cta_bg"
    )

    Pressable(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        pressedScale = 0.97f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Banana.Radius.l))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "cta_state"
            ) { isLoading ->
                if (isLoading) {
                    BananaLoader()
                } else {
                    Text(
                        text,
                        style = Banana.Type.headline,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Three-dot pulsing loader
@Composable
private fun BananaLoader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { idx ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(idx * 130L)
                visible = true
            }
            val transition = rememberInfiniteTransition(label = "dot_$idx")
            val s by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(540, delayMillis = idx * 130, easing = Banana.Motion.EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale_$idx"
            )
            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(180), label = "a_$idx"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(s)
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
