package com.vixcy.banana

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vixcy.banana.ui.Banana
import com.vixcy.banana.ui.BananaField
import com.vixcy.banana.ui.Pressable
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount   by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val parsedAmount = amount.toDoubleOrNull()
    val canSave = parsedAmount != null && parsedAmount > 0 &&
            merchant.isNotBlank() && !isSaving

    // Staggered entrance — content fades + slides after sheet settles
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        entered = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Banana.Color.Bg,
        shape = RoundedCornerShape(
            topStart = Banana.Radius.xxl,
            topEnd = Banana.Radius.xxl
        ),
        dragHandle = { BananaDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Banana.Space.xxl,
                    end = Banana.Space.xxl,
                    bottom = 48.dp
                )
        ) {
            // ── Header ────────────────────────────────────────────────────
            EnterAnim(visible = entered, delayMs = 0) {
                Text(
                    "Add expense",
                    style = Banana.Type.title,
                    color = Banana.Color.Ink
                )
            }

            Spacer(Modifier.height(Banana.Space.xs))

            EnterAnim(visible = entered, delayMs = 60) {
                Text(
                    "Quick log — categorize automatically.",
                    style = Banana.Type.body,
                    color = Banana.Color.InkSecondary
                )
            }

            Spacer(Modifier.height(Banana.Space.l))

            // ── Live amount preview (counts when keyboard fills) ─────────
            EnterAnim(visible = entered, delayMs = 120) {
                AmountPreview(parsedAmount = parsedAmount)
            }

            Spacer(Modifier.height(Banana.Space.m))

            // ── Quick-amount preset chips ─────────────────────────────────
            EnterAnim(visible = entered, delayMs = 180) {
                QuickAmountRow(
                    selected = parsedAmount,
                    onSelect = { amount = it.toInt().toString() }
                )
            }

            Spacer(Modifier.height(Banana.Space.l))

            // ── Amount input ─────────────────────────────────────────────
            EnterAnim(visible = entered, delayMs = 240) {
                BananaField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = "0",
                    leading = {
                        Text(
                            "₹",
                            style = Banana.Type.headline,
                            color = Banana.Color.InkSecondary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = Banana.Type.headline.copy(color = Banana.Color.Ink),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(Banana.Space.s))

            // ── Merchant input ───────────────────────────────────────────
            EnterAnim(visible = entered, delayMs = 300) {
                BananaField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = "Merchant or person",
                    textStyle = Banana.Type.headline.copy(color = Banana.Color.Ink),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(Banana.Space.xl))

            // ── Save CTA ─────────────────────────────────────────────────
            EnterAnim(visible = entered, delayMs = 360) {
                SaveButton(
                    enabled = canSave,
                    isSaving = isSaving,
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: return@SaveButton
                        if (merchant.isBlank()) return@SaveButton
                        isSaving = true
                        val tx = Transaction(
                            amount = amt,
                            merchant = merchant.uppercase().trim(),
                            date = java.text.SimpleDateFormat(
                                "dd-MM-yy", java.util.Locale.getDefault()
                            ).format(java.util.Date()),
                            accountLast4 = "Manual",
                            rawSms = "manual_entry",
                            upiRef = "manual_${System.currentTimeMillis()}"
                        )
                        onSave(tx)
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drag handle — narrow pill in subtle ink
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BananaDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 38.dp, height = 4.dp)
            .clip(RoundedCornerShape(Banana.Radius.pill))
            .background(Banana.Color.InkSubtle)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated entrance wrapper — fades + slides up with custom delay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EnterAnim(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(380, delayMillis = delayMs, easing = Banana.Motion.EaseOut)
        ) + slideInVertically(
            animationSpec = tween(440, delayMillis = delayMs, easing = Banana.Motion.EaseOut)
        ) { it / 6 }
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Amount preview — large display number that animates between values
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AmountPreview(parsedAmount: Double?) {
    AnimatedContent(
        targetState = parsedAmount,
        transitionSpec = {
            (fadeIn(tween(180, easing = Banana.Motion.EaseOut)) +
                    slideInVertically(tween(220, easing = Banana.Motion.EaseOut)) { -it / 6 })
                .togetherWith(
                    fadeOut(tween(120)) +
                            slideOutVertically(tween(160)) { it / 6 }
                )
        },
        label = "amount_preview"
    ) { amt ->
        val active = amt != null && amt > 0
        Text(
            text = if (active) "₹${"%,.0f".format(amt)}" else "₹0",
            style = Banana.Type.display.copy(fontSize = 48.sp),
            color = if (active) Banana.Color.Ink else Banana.Color.InkMuted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick-amount chips — tap to autofill common values
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickAmountRow(
    selected: Double?,
    onSelect: (Double) -> Unit
) {
    val presets = listOf(50, 100, 200, 500, 1000, 2000)
    Row(horizontalArrangement = Arrangement.spacedBy(Banana.Space.s)) {
        presets.forEach { value ->
            QuickAmountChip(
                value = value,
                active = selected?.toInt() == value,
                onClick = { onSelect(value.toDouble()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickAmountChip(
    value: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (active) Banana.Color.Ink else Banana.Color.Surface,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "qa_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (active) Color.White else Banana.Color.Ink,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "qa_fg"
    )
    Pressable(onClick = onClick, modifier = modifier, pressedScale = 0.94f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(Banana.Radius.s))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$value",
                style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Save button — gradient when enabled, dimmed when not, three-dot loader on save
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    val gradientAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "save_alpha"
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
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Banana.Color.Accent.copy(alpha = gradientAlpha),
                            Banana.Color.AccentDeep.copy(alpha = gradientAlpha)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isSaving,
                transitionSpec = {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(110))
                },
                label = "save_state"
            ) { saving ->
                if (saving) {
                    SaveLoader()
                } else {
                    Text(
                        "Save expense",
                        style = Banana.Type.headline,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Three-dot pulsing loader — same vocabulary as AuthScreen's loader
@Composable
private fun SaveLoader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { idx ->
            val transition = rememberInfiniteTransition(label = "save_dot_$idx")
            val s by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(540, delayMillis = idx * 130, easing = Banana.Motion.EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale_$idx"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(s)
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(Color.White)
            )
        }
    }
}
