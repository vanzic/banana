package com.vixcy.banana

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vixcy.banana.ui.AccentChip
import com.vixcy.banana.ui.Banana
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    tx: Transaction,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val category = remember(tx) { NotificationParser.categorize(tx.merchant) }

    // Counts up from 0 → amount (same trick used on the dashboard hero)
    var target by remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(700, easing = Banana.Motion.EaseOut),
        label = "tx_amount"
    )
    LaunchedEffect(tx) {
        delay(40)
        target = tx.amount.toFloat()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Banana.Color.Bg,
        shape = RoundedCornerShape(topStart = Banana.Radius.xxl, topEnd = Banana.Radius.xxl),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(Banana.Color.InkSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Banana.Space.xxl)
                .padding(bottom = 48.dp)
        ) {
            // Big merchant icon + amount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(Banana.Radius.xl))
                            .background(Banana.Color.AccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(merchantEmoji(tx.merchant), fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        tx.merchant,
                        style = Banana.Type.title,
                        color = Banana.Color.Ink
                    )
                    Spacer(Modifier.height(6.dp))
                    AccentChip(
                        text = category,
                        bg = categoryBg(category),
                        fg = categoryFg(category)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "−₹${"%,.0f".format(animated)}",
                        style = Banana.Type.display.copy(fontSize = 56.sp),
                        color = Banana.Color.Ink
                    )
                }
            }

            // Detail card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Banana.Radius.xl))
                    .background(Banana.Color.Surface)
                    .padding(horizontal = 4.dp)
            ) {
                DetailRow(label = "Date", value = tx.date)
                Divider()
                DetailRow(
                    label = "Account",
                    value = if (tx.accountLast4 == "Manual" || tx.accountLast4 == "UPI" || tx.accountLast4 == "SMS")
                        tx.accountLast4 else "•• ${tx.accountLast4}"
                )
                Divider()
                DetailRow(
                    label = "Source",
                    value = when {
                        tx.upiRef.startsWith("manual_") -> "Manual entry"
                        tx.upiRef.isNotBlank() -> "UPI · ${tx.upiRef.take(12)}"
                        else -> "Bank SMS"
                    }
                )
                if (tx.rawSms.isNotBlank() && tx.rawSms != "manual_entry") {
                    Divider()
                    DetailRow(label = "Raw message", value = tx.rawSms, multiline = true)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, multiline: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = Banana.Type.callout,
            color = Banana.Color.InkSecondary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            value,
            style = if (multiline)
                Banana.Type.footnote.copy(fontWeight = FontWeight.Normal)
            else
                Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
            color = Banana.Color.Ink,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = if (multiline) 6 else 1
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Banana.Color.StrokeSoft,
        thickness = 0.5.dp
    )
}

internal fun categoryBg(category: String) = when (category.lowercase()) {
    "food"          -> Banana.Color.AccentSoft
    "transport"     -> Banana.Color.SkySoft
    "shopping"      -> Banana.Color.LavenderSoft
    "entertainment" -> Banana.Color.MintSoft
    "utilities"     -> Banana.Color.SandSoft
    "health"        -> Banana.Color.ErrorSoft
    else            -> Banana.Color.SurfaceDim
}

internal fun categoryFg(category: String) = when (category.lowercase()) {
    "food"          -> Banana.Color.Accent
    "transport"     -> Banana.Color.Sky
    "shopping"      -> Banana.Color.Lavender
    "entertainment" -> Banana.Color.Mint
    "utilities"     -> Banana.Color.Sand
    "health"        -> Banana.Color.Error
    else            -> Banana.Color.InkSecondary
}
