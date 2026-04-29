package com.vixcy.banana

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vixcy.banana.ui.AccentChip
import com.vixcy.banana.ui.Banana
import com.vixcy.banana.ui.Pressable
import com.vixcy.banana.ui.bananaShadow
import com.vixcy.banana.ui.bananaShadowFab
import com.vixcy.banana.ui.shimmer
import com.vixcy.banana.ui.theme.BananaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        requestSmsPermission()

        Thread { SmsImporter.importExistingSms(this) }.start()

        setContent {
            BananaTheme {
                var isLoggedIn by remember { mutableStateOf(AuthManager.isLoggedIn) }

                // Crossfade auth ↔ dashboard with subtle scale (spatial continuity)
                AnimatedContent(
                    targetState = isLoggedIn,
                    transitionSpec = {
                        (fadeIn(tween(420, easing = Banana.Motion.EaseOut)) +
                                scaleIn(initialScale = 0.96f, animationSpec = tween(420, easing = Banana.Motion.EaseOut)))
                            .togetherWith(
                                fadeOut(tween(220)) +
                                        scaleOut(targetScale = 1.04f, animationSpec = tween(280))
                            )
                    },
                    label = "auth_to_dashboard"
                ) { logged ->
                    if (logged) {
                        DashboardScreen(
                            onSignOut = {
                                AuthManager.signOut()
                                isLoggedIn = false
                            }
                        )
                    } else {
                        AuthScreen(onAuthSuccess = { isLoggedIn = true })
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        val enabled = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        if (!enabled) startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestSmsPermission() {
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        if (readSms != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onSignOut: () -> Unit = {}) {
    val transactions = remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var detailTx by remember { mutableStateOf<Transaction?>(null) }

    var heroVisible by remember { mutableStateOf(false) }
    var statsVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }
    var transactionsVisible by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    // Tick increments to retrigger the burst overlay each save
    var saveBurstTick by remember { mutableStateOf(0) }

    fun loadTransactions(onDone: () -> Unit = {}) {
        SupabaseFetcher.fetchTransactions { result ->
            transactions.value = result
            isLoading.value = false
            onDone()
        }
    }

    // Optimistic delete: remove from local list immediately so the UI feels
    // instant; restore on backend failure.
    fun deleteTransaction(tx: Transaction) {
        val before = transactions.value
        transactions.value = before.filterNot {
            it.upiRef == tx.upiRef &&
                    it.amount == tx.amount &&
                    it.date == tx.date &&
                    it.merchant == tx.merchant
        }
        SupabaseClient.deleteTransaction(tx) { ok ->
            if (!ok) transactions.value = before
        }
    }

    LaunchedEffect(Unit) {
        loadTransactions()
        delay(220)
        fabVisible = true
    }

    LaunchedEffect(isLoading.value) {
        if (!isLoading.value) {
            heroVisible = true
            delay(80)
            statsVisible = true
            delay(70)
            categoriesVisible = true
            delay(70)
            transactionsVisible = true
        }
    }

    val totalSpent = transactions.value.sumOf { it.amount }
    val categoryTotals = transactions.value
        .groupBy { NotificationParser.categorize(it.merchant) }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
    val maxCat = categoryTotals.firstOrNull()?.value ?: 1.0
    val grouped = remember(transactions.value) { groupByDay(transactions.value) }

    val scrollState = rememberScrollState()
    // 0..1 progress used to compress the hero and reveal the sticky bar
    val scrollProgress = (scrollState.value / 280f).coerceIn(0f, 1f)

    val pullState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Banana.Color.Bg)
    ) {
        when {
            isLoading.value && transactions.value.isEmpty() -> LoadingState()
            transactions.value.isEmpty() -> EmptyState(visible = heroVisible)
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        loadTransactions { isRefreshing = false }
                    },
                    state = pullState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 120.dp)
                    ) {
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        Spacer(Modifier.height(56.dp)) // headroom for sticky bar

                        // ── Hero ───────────────────────────────────────────
                        AnimatedVisibility(
                            visible = heroVisible,
                            enter = fadeIn(tween(540, easing = Banana.Motion.EaseOut)) +
                                    slideInVertically(tween(620, easing = Banana.Motion.EaseOut)) { it / 7 }
                        ) {
                            HeroCard(
                                totalSpent = totalSpent,
                                transactionCount = transactions.value.size,
                                scrollProgress = scrollProgress,
                                onSignOut = onSignOut
                            )
                        }

                        Spacer(Modifier.height(Banana.Space.m))

                        // ── Stat strip ─────────────────────────────────────
                        AnimatedVisibility(
                            visible = statsVisible,
                            enter = fadeIn(tween(440, easing = Banana.Motion.EaseOut)) +
                                    slideInVertically(tween(520, easing = Banana.Motion.EaseOut)) { it / 8 }
                        ) {
                            StatStrip(
                                transactions = transactions.value,
                                scrollProgress = scrollProgress
                            )
                        }

                        Spacer(Modifier.height(Banana.Space.m))

                        // ── Category breakdown ─────────────────────────────
                        AnimatedVisibility(
                            visible = categoriesVisible,
                            enter = fadeIn(tween(440, easing = Banana.Motion.EaseOut)) +
                                    slideInVertically(tween(520, easing = Banana.Motion.EaseOut)) { it / 8 }
                        ) {
                            CategoryCard(
                                categoryTotals = categoryTotals,
                                maxCat = maxCat
                            )
                        }

                        Spacer(Modifier.height(Banana.Space.m))

                        // ── Transactions, day-grouped ──────────────────────
                        AnimatedVisibility(
                            visible = transactionsVisible,
                            enter = fadeIn(tween(440, easing = Banana.Motion.EaseOut)) +
                                    slideInVertically(tween(520, easing = Banana.Motion.EaseOut)) { it / 8 }
                        ) {
                            TransactionsCard(
                                grouped = grouped,
                                onTap = { detailTx = it },
                                onDelete = { tx -> deleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }

        // ── Sticky morphing top bar ───────────────────────────────────────
        StickyTopBar(
            scrollProgress = scrollProgress,
            totalSpent = totalSpent,
            onSignOut = { showSignOutConfirm = true }
        )

        // ── FAB ───────────────────────────────────────────────────────────
        BananaFab(
            visible = fabVisible,
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 24.dp, bottom = 24.dp)
        )

        if (showSheet) {
            ManualEntrySheet(
                onDismiss = { showSheet = false },
                onSave = { tx ->
                    showSheet = false
                    saveBurstTick++          // fire celebration burst
                    insertOptimistic(transactions, tx)
                }
            )
        }

        // ── Save celebration — radiates dots from above the FAB ──────────
        SaveBurst(
            tick = saveBurstTick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 54.dp, bottom = 54.dp)
        )

        detailTx?.let {
            TransactionDetailSheet(tx = it, onDismiss = { detailTx = null })
        }

        if (showSignOutConfirm) {
            SignOutConfirmDialog(
                onConfirm = {
                    showSignOutConfirm = false
                    onSignOut()
                },
                onDismiss = { showSignOutConfirm = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sticky top bar — fades in as the hero scrolls away
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.StickyTopBar(
    scrollProgress: Float,
    totalSpent: Double,
    onSignOut: () -> Unit
) {
    val barAlpha by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = tween(120),
        label = "bar_alpha"
    )
    val titleY by animateFloatAsState(
        targetValue = (1f - scrollProgress) * 12f,
        animationSpec = tween(120),
        label = "title_y"
    )

    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            // Layered "glass" — base bg fades in, then a luminous overlay
            // (subtle white at low alpha) gives the tinted-frosted look that
            // resembles iOS UIBlurEffect.systemUltraThinMaterial.
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Banana.Color.Bg.copy(alpha = barAlpha * 0.94f),
                        Banana.Color.Bg.copy(alpha = barAlpha * 0.86f)
                    )
                )
            )
            .background(Color.White.copy(alpha = barAlpha * 0.18f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Banana.Space.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = titleY
                        alpha = barAlpha
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🍌", fontSize = 16.sp)
                Text(
                    "₹${"%,.0f".format(totalSpent)}",
                    style = Banana.Type.headline,
                    color = Banana.Color.Ink
                )
            }
            // Sign-out — text pill, always tappable. Glyph-free so it
            // never falls back to a missing-font box on certain devices.
            Pressable(onClick = onSignOut, pressedScale = 0.94f) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Banana.Radius.pill))
                        .background(Banana.Color.SurfaceDim)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "Sign out",
                        style = Banana.Type.captionBold,
                        color = Banana.Color.InkSecondary
                    )
                }
            }
        }
        // Subtle hairline divider that appears with the bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Banana.Color.Stroke.copy(alpha = barAlpha))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero — parallax compresses + fades as user scrolls
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroCard(
    totalSpent: Double,
    transactionCount: Int,
    scrollProgress: Float,
    onSignOut: () -> Unit
) {
    // Spring-based count-up — slight overshoot then settle. Feels satisfying
    // when a fresh load lands, way more "alive" than a tween.
    var target by remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 90f),
        label = "amount_counter"
    )

    // When the count-up settles, briefly pulse the amount up 4% as a finisher.
    var pulse by remember { mutableStateOf(false) }
    val pulseScale by animateFloatAsState(
        targetValue = if (pulse) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 320f),
        label = "amount_pulse"
    )
    LaunchedEffect(totalSpent) {
        delay(70)
        target = totalSpent.toFloat()
        delay(820)            // wait for the count-up to almost settle
        pulse = true
        delay(220)
        pulse = false
    }

    // Idle breathing — the amount is never perfectly static (very iOS).
    val breathe = rememberInfiniteTransition(label = "hero_breathe")
    val breath by breathe.animateFloat(
        initialValue = 0.998f,
        targetValue = 1.004f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val monthName = remember {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.l)
            .graphicsLayer {
                val s = 1f - scrollProgress * 0.06f
                scaleX = s; scaleY = s
                alpha = 1f - scrollProgress * 0.55f
                translationY = -scrollProgress * 24f
            }
            .bananaShadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(Banana.Radius.xxl),
                spot = Color(0x1AC74A23)   // warm shadow tint matches the card warmth
            )
            .clip(RoundedCornerShape(Banana.Radius.xxl))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Banana.Color.Surface,
                        Banana.Color.Surface.copy(alpha = 0.99f),
                        Banana.Color.AccentSofter
                    )
                )
            )
    ) {
        Column(modifier = Modifier.padding(Banana.Space.xxl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🍌", fontSize = 14.sp)
                    Text(
                        "Wallet",
                        style = Banana.Type.callout,
                        color = Banana.Color.InkSecondary
                    )
                }
                AccentChip(
                    text = monthName,
                    bg = Banana.Color.SurfaceDim,
                    fg = Banana.Color.InkSecondary,
                    style = Banana.Type.captionBold
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "TOTAL SPENT",
                style = Banana.Type.tagLabel,
                color = Banana.Color.InkMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "₹${"%,.0f".format(animated)}",
                style = Banana.Type.display,
                color = Banana.Color.Ink,
                modifier = Modifier.graphicsLayer {
                    val s = breath * pulseScale
                    scaleX = s; scaleY = s
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Banana.Color.StrokeSoft, thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$transactionCount transactions",
                    style = Banana.Type.footnote,
                    color = Banana.Color.InkMuted
                )
                AccentChip("this month")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat strip — 2 cards (Biggest, Most visits) with press feedback
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatStrip(transactions: List<Transaction>, scrollProgress: Float = 0f) {
    val biggest = transactions.maxByOrNull { it.amount }
    val mostFrequent = transactions
        .groupingBy { it.merchant }
        .eachCount()
        .maxByOrNull { it.value }
    val avgDaily = remember(transactions) {
        val byDay = transactions.groupBy { it.date }
        if (byDay.isEmpty()) 0.0
        else transactions.sumOf { it.amount } / byDay.size
    }

    Row(
        modifier = Modifier
            .padding(horizontal = Banana.Space.l)
            // Slight parallax — moves up & dims as you scroll past the hero.
            .graphicsLayer {
                alpha = 1f - scrollProgress * 0.35f
                translationY = -scrollProgress * 14f
            },
        horizontalArrangement = Arrangement.spacedBy(Banana.Space.s)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = "🔥",
            iconBg = Banana.Color.AccentSoft,
            iconFg = Banana.Color.Accent,
            label = "BIGGEST",
            value = "₹${"%,.0f".format(biggest?.amount ?: 0.0)}",
            sub = biggest?.merchant ?: "—"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = "↻",
            iconBg = Banana.Color.LavenderSoft,
            iconFg = Banana.Color.Lavender,
            label = "MOST VISITS",
            value = mostFrequent?.key ?: "—",
            sub = "${mostFrequent?.value ?: 0} times"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = "≈",
            iconBg = Banana.Color.MintSoft,
            iconFg = Banana.Color.Mint,
            label = "DAILY AVG",
            value = "₹${"%,.0f".format(avgDaily)}",
            sub = "per active day"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: String,
    iconBg: Color,
    iconFg: Color,
    label: String,
    value: String,
    sub: String
) {
    Pressable(onClick = {}, modifier = modifier, pressedScale = 0.96f) {
        Column(
            modifier = Modifier
                .bananaShadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(Banana.Radius.xl)
                )
                .clip(RoundedCornerShape(Banana.Radius.xl))
                // Subtle top-light → bottom-warm gradient gives Apple-card depth
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Banana.Color.Surface,
                            Banana.Color.Surface.copy(alpha = 0.985f),
                            Banana.Color.SurfaceDim.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(Banana.Space.l)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(Banana.Radius.s))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 16.sp, color = iconFg, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                label,
                style = Banana.Type.tagLabel.copy(letterSpacing = 0.8.sp),
                color = Banana.Color.InkMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = Banana.Type.headline,
                color = Banana.Color.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                style = Banana.Type.footnote,
                color = Banana.Color.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spending breakdown card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryCard(
    categoryTotals: List<Map.Entry<String, Double>>,
    maxCat: Double
) {
    val total = categoryTotals.sumOf { it.value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.l)
            .bananaShadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(Banana.Radius.xl)
            )
            .clip(RoundedCornerShape(Banana.Radius.xl))
            .background(Banana.Color.Surface)
            .padding(Banana.Space.xl)
    ) {
        Text(
            "Spending breakdown",
            style = Banana.Type.headline,
            color = Banana.Color.Ink
        )
        Spacer(Modifier.height(Banana.Space.l))

        categoryTotals.take(5).forEachIndexed { idx, entry ->
            CategoryBar(
                name = entry.key,
                amount = entry.value,
                fraction = (entry.value / maxCat).toFloat(),
                percent = if (total > 0) (entry.value / total * 100).toInt() else 0,
                color = categoryFg(entry.key),
                bg = categoryBg(entry.key),
                staggerIndex = idx
            )
            if (idx < categoryTotals.take(5).lastIndex)
                Spacer(Modifier.height(Banana.Space.l))
        }
    }
}

@Composable
private fun CategoryBar(
    name: String,
    amount: Double,
    fraction: Float,
    percent: Int,
    color: Color,
    bg: Color,
    staggerIndex: Int
) {
    var target by remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(0.62f, Spring.StiffnessMediumLow),
        label = "bar"
    )
    LaunchedEffect(fraction) {
        delay(staggerIndex * 60L)
        target = fraction
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(name, style = Banana.Type.bodyMedium, color = Banana.Color.Ink)
                AccentChip(
                    text = "$percent%",
                    bg = bg,
                    fg = color,
                    horizontalPadding = 8.dp,
                    verticalPadding = 2.dp,
                    style = Banana.Type.tagLabel.copy(letterSpacing = 0.5.sp)
                )
            }
            Text(
                "₹${"%,.0f".format(amount)}",
                style = Banana.Type.bodyMedium,
                color = Banana.Color.InkSecondary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(Banana.Radius.pill))
                .background(Banana.Color.SurfaceDim)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(6.dp)
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(color)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transactions, grouped by day. Each row supports iOS-style swipe-to-delete.
// Only one row may be open at a time (Apple Phone-app behavior).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TransactionsCard(
    grouped: List<DayGroup>,
    onTap: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    // The key of the currently-open swipe row (null = all closed).
    var openKey by remember { mutableStateOf<String?>(null) }
    // Keys whose row is mid-removal animation. They stay rendered until the
    // exit transition finishes, then the parent strips them from the data list.
    val deletingKeys = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.l)
            .bananaShadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(Banana.Radius.xl)
            )
            .clip(RoundedCornerShape(Banana.Radius.xl))
            .background(Banana.Color.Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Banana.Space.xl, end = Banana.Space.xl,
                         top = Banana.Space.xl, bottom = Banana.Space.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activity", style = Banana.Type.headline, color = Banana.Color.Ink)
            Pressable(onClick = {}, pressedScale = 0.92f) {
                Text(
                    "See all",
                    style = Banana.Type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Banana.Color.Accent
                )
            }
        }

        var rowIndex = 0
        grouped.forEachIndexed { gIdx, group ->
            DayHeader(label = group.label, total = group.total)
            group.items.forEachIndexed { idx, tx ->
                val myIndex = rowIndex
                // Stable across list mutations (no positional `idx` in the key).
                // upiRef is unique for manual entries (manual_<ts>) and real UPI
                // refs; we fall back to a content fingerprint for legacy rows
                // with no upiRef, plus group/idx as a tie-breaker for true dupes.
                val rowKey = if (tx.upiRef.isNotBlank()) tx.upiRef
                else "${tx.date}|${tx.merchant}|${tx.amount}|$gIdx|$idx"
                key(rowKey) {
                    var entered by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(myIndex * 32L)
                        entered = true
                    }

                    val visible = entered && rowKey !in deletingKeys

                    AnimatedVisibility(
                        visible = visible,
                        // The crucial bit is `expandVertically`: without it the
                        // layout slot opens to full height in one frame, which
                        // shoves all the rows below down with a visible jolt
                        // (the "dashboard pulled down" feel). Expanding the
                        // slot smoothly lets neighbors flow down naturally.
                        enter = fadeIn(tween(240, easing = Banana.Motion.EaseOut)) +
                                expandVertically(
                                    animationSpec = tween(260, easing = Banana.Motion.EaseOut)
                                ) +
                                slideInVertically(
                                    animationSpec = tween(280, easing = Banana.Motion.EaseOut)
                                ) { -it / 6 },   // settles down from above
                        exit = shrinkVertically(
                            animationSpec = tween(280, easing = Banana.Motion.EaseOut)
                        ) + fadeOut(tween(200))
                    ) {
                        SwipeableTransactionRow(
                            tx = tx,
                            isOpen = openKey == rowKey,
                            onOpenChange = { open ->
                                openKey = if (open) rowKey else null
                            },
                            onTap = { onTap(tx) },
                            onDelete = {
                                openKey = null
                                deletingKeys.add(rowKey)
                                scope.launch {
                                    // wait for the AnimatedVisibility shrink-out to play
                                    delay(320)
                                    onDelete(tx)
                                    deletingKeys.remove(rowKey)
                                }
                            }
                        )
                    }
                    if (idx < group.items.lastIndex && visible) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = Banana.Space.xl),
                            color = Banana.Color.StrokeSoft,
                            thickness = 0.5.dp
                        )
                    }
                }
                rowIndex++
            }
            if (gIdx < grouped.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Banana.Space.xl),
                    color = Banana.Color.Stroke.copy(alpha = 0.6f),
                    thickness = 0.5.dp
                )
            }
        }
        Spacer(Modifier.height(Banana.Space.m))
    }
}

@Composable
private fun DayHeader(label: String, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.xl, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            style = Banana.Type.tagLabel,
            color = Banana.Color.InkMuted
        )
        Text(
            "₹${"%,.0f".format(total)}",
            style = Banana.Type.tagLabel,
            color = Banana.Color.InkMuted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SwipeableTransactionRow — iOS-style swipe-to-delete.
//
//  • drag left → red Delete pill is revealed (84dp wide)
//  • the "Delete" label parallaxes from the right edge as you drag
//  • release < half → snap closed; release > half → snap open
//  • drag past the full-swipe threshold → haptic snap, release deletes directly
//  • only one row can be open at a time (Apple Phone-app behavior)
//  • tapping the row body when open → snap closed; when closed → onTap()
//  • when delete is committed: row slides off, then parent's AnimatedVisibility
//    shrinks the height. Neighbors flow up naturally.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SwipeableTransactionRow(
    tx: Transaction,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val deleteWidthDp = 84.dp
    val deleteWidthPx = with(density) { deleteWidthDp.toPx() }
    val rubberBandPx  = with(density) { 28.dp.toPx() }            // small over-drag past open
    val fullSwipeThresholdPx = with(density) { 220.dp.toPx() }    // full-swipe = direct delete

    var offset by remember { mutableFloatStateOf(0f) }
    var isCommitting by remember { mutableStateOf(false) }
    var hapticArmed by remember { mutableStateOf(true) }

    // External close — when another row opens or the parent demands close.
    LaunchedEffect(isOpen) {
        if (!isOpen && offset < 0f && !isCommitting) {
            animate(
                initialValue = offset,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f)
            ) { v, _ -> offset = v }
        }
    }

    val swipeProgress = (-offset / deleteWidthPx).coerceIn(0f, 1f)
    val isFullSwipe = -offset > fullSwipeThresholdPx

    val pressInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── Red action layer (sits behind the row) ──────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(0.dp))
                .background(Banana.Color.Error)
        ) {
            // Active "Delete" tap zone. Anchors to the right edge and matches
            // the revealed amount, so during a full-swipe the whole left edge
            // remains a Delete tap target — exactly like iOS.
            val revealedDp = with(density) { (-offset).coerceAtLeast(0f).toDp() }
            val tapWidth = if (isFullSwipe) revealedDp else deleteWidthDp

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(tapWidth)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isCommitting) return@clickable
                        isCommitting = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            // Slide the row off-screen smoothly
                            animate(
                                initialValue = offset,
                                targetValue = -3000f,
                                animationSpec = tween(220, easing = Banana.Motion.EaseOut)
                            ) { v, _ -> offset = v }
                            onDelete()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Delete label — translates in from the right with parallax,
                // fades & gently scales as you reveal it.
                val labelTranslate = (1f - swipeProgress) * 26f
                val labelScale = 0.85f + 0.15f * swipeProgress
                Text(
                    "Delete",
                    style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        translationX = labelTranslate
                        alpha = swipeProgress
                        scaleX = labelScale
                        scaleY = labelScale
                    }
                )
            }
        }

        // ── Foreground row (slides left, casts the red "underneath") ────
        val pressed by pressInteraction.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (pressed && offset > -1f) 0.97f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "row_press"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) }
                .scale(pressScale)
                .background(Banana.Color.Surface)
                .pointerInput(tx.upiRef + tx.date) {
                    detectHorizontalDragGestures(
                        onDragStart = { hapticArmed = true },
                        onDragEnd = {
                            if (isCommitting) return@detectHorizontalDragGestures
                            scope.launch {
                                if (-offset > fullSwipeThresholdPx) {
                                    // Past full-swipe → commit delete directly
                                    isCommitting = true
                                    animate(
                                        initialValue = offset,
                                        targetValue = -3000f,
                                        animationSpec = tween(200, easing = Banana.Motion.EaseOut)
                                    ) { v, _ -> offset = v }
                                    onDelete()
                                } else {
                                    val target = if (-offset > deleteWidthPx / 2f)
                                        -deleteWidthPx else 0f
                                    animate(
                                        initialValue = offset,
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = 700f
                                        )
                                    ) { v, _ -> offset = v }
                                    onOpenChange(target < 0f)
                                }
                            }
                        },
                        onDragCancel = {
                            if (isCommitting) return@detectHorizontalDragGestures
                            scope.launch {
                                animate(
                                    initialValue = offset,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = 700f
                                    )
                                ) { v, _ -> offset = v }
                                onOpenChange(false)
                            }
                        },
                        onHorizontalDrag = { change, dx ->
                            if (isCommitting) return@detectHorizontalDragGestures
                            change.consume()
                            // Allow a slight rubber-band over-drag past the open position
                            // for tactile feel, but never let the row drag right of 0.
                            val raw = (offset + dx).coerceAtMost(0f)
                            offset = raw
                            // Haptic at the full-swipe threshold (re-arms after retreat)
                            if (-raw > fullSwipeThresholdPx && hapticArmed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hapticArmed = false
                            } else if (-raw < fullSwipeThresholdPx - rubberBandPx) {
                                hapticArmed = true
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = pressInteraction,
                    indication = null
                ) {
                    if (offset < -1f) {
                        // Open → tapping the body snaps it closed (iOS pattern)
                        scope.launch {
                            animate(
                                initialValue = offset,
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 700f
                                )
                            ) { v, _ -> offset = v }
                            onOpenChange(false)
                        }
                    } else {
                        onTap()
                    }
                }
        ) {
            TransactionRowContent(tx = tx)
        }
    }
}

// Pure-visual row content. No interaction — that's owned by the swipe wrapper.
@Composable
private fun TransactionRowContent(tx: Transaction) {
    val category = remember(tx.merchant) { NotificationParser.categorize(tx.merchant) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.xl, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Banana.Radius.m))
                .background(categoryBg(category)),
            contentAlignment = Alignment.Center
        ) {
            Text(merchantEmoji(tx.merchant), fontSize = 18.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                formatMerchant(tx.merchant),
                style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = Banana.Color.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                category,
                style = Banana.Type.footnote,
                color = Banana.Color.InkMuted
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "−₹${"%,.0f".format(tx.amount)}",
                style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                color = Banana.Color.Ink
            )
            Text(
                when {
                    tx.accountLast4 == "Manual" -> "Manual"
                    tx.accountLast4 == "UPI" -> "UPI"
                    else -> "•• ${tx.accountLast4}"
                },
                style = Banana.Type.footnote,
                color = Banana.Color.InkSubtle
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB — spring entrance + press squish
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BananaFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entrance by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(0.55f, Spring.StiffnessMedium),
        label = "fab_in"
    )
    Pressable(onClick = onClick, modifier = modifier, pressedScale = 0.88f) {
        Box(
            modifier = Modifier
                .scale(entrance)
                .size(60.dp)
                .bananaShadowFab()
                .clip(RoundedCornerShape(Banana.Radius.l))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Banana.Color.Ink,
                            Banana.Color.InkSoft
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Light)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SaveBurst — 8 small accent dots that radiate outward + fade. A subtle
// celebration when a manual expense saves successfully.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SaveBurst(tick: Int, modifier: Modifier = Modifier) {
    if (tick == 0) return
    val progress = remember(tick) { Animatable(0f) }
    LaunchedEffect(tick) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(720, easing = Banana.Motion.EaseOut)
        )
    }

    Box(modifier = modifier.size(1.dp)) {
        repeat(8) { i ->
            val angle = (i * 360f / 8f) + (tick * 13f) // rotate slightly each fire
            val rad = Math.toRadians(angle.toDouble())
            val maxRadiusPx = with(LocalDensity.current) { 56.dp.toPx() }
            val r = maxRadiusPx * progress.value
            val dx = (r * cos(rad)).toFloat()
            val dy = (r * sin(rad)).toFloat()
            val alpha = 1f - progress.value
            val scale = 1f - 0.3f * progress.value

            // Alternate accent / mint dots for a touch of variety
            val color = if (i % 2 == 0) Banana.Color.Accent else Banana.Color.Mint

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        translationX = dx
                        translationY = dy
                        this.alpha = alpha
                        scaleX = scale; scaleY = scale
                    }
                    .clip(RoundedCornerShape(Banana.Radius.pill))
                    .background(color)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading state — shimmering skeleton hero + 3 fake rows.
// Mirrors the real layout so the transition into real content feels like
// the data simply "tunes in" instead of jumping.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.TopCenter)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(56.dp + 12.dp))   // matches sticky-bar headroom
        SkeletonHero()
        Spacer(Modifier.height(Banana.Space.m))
        SkeletonStatStrip()
        Spacer(Modifier.height(Banana.Space.m))
        SkeletonActivity()
    }
}

@Composable
private fun SkeletonHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.l)
            .bananaShadow(elevation = 14.dp, shape = RoundedCornerShape(Banana.Radius.xxl))
            .clip(RoundedCornerShape(Banana.Radius.xxl))
            .background(Banana.Color.Surface)
            .padding(Banana.Space.xxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBlock(width = 90.dp, height = 14.dp)
            ShimmerBlock(width = 86.dp, height = 22.dp, radius = 999.dp)
        }
        Spacer(Modifier.height(28.dp))
        ShimmerBlock(width = 80.dp, height = 10.dp)
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(width = 220.dp, height = 44.dp)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Banana.Color.StrokeSoft, thickness = 0.5.dp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBlock(width = 100.dp, height = 12.dp)
            ShimmerBlock(width = 80.dp, height = 22.dp, radius = 999.dp)
        }
    }
}

@Composable
private fun SkeletonStatStrip() {
    Row(
        modifier = Modifier.padding(horizontal = Banana.Space.l),
        horizontalArrangement = Arrangement.spacedBy(Banana.Space.s)
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .bananaShadow(elevation = 6.dp, shape = RoundedCornerShape(Banana.Radius.xl))
                    .clip(RoundedCornerShape(Banana.Radius.xl))
                    .background(Banana.Color.Surface)
                    .padding(Banana.Space.l)
            ) {
                ShimmerBlock(width = 36.dp, height = 36.dp, radius = Banana.Radius.s)
                Spacer(Modifier.height(14.dp))
                ShimmerBlock(width = 70.dp, height = 10.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBlock(width = 80.dp, height = 16.dp)
                Spacer(Modifier.height(4.dp))
                ShimmerBlock(width = 60.dp, height = 10.dp)
            }
        }
    }
}

@Composable
private fun SkeletonActivity() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.l)
            .bananaShadow(elevation = 6.dp, shape = RoundedCornerShape(Banana.Radius.xl))
            .clip(RoundedCornerShape(Banana.Radius.xl))
            .background(Banana.Color.Surface)
            .padding(vertical = Banana.Space.l)
    ) {
        ShimmerBlock(
            width = 80.dp, height = 16.dp,
            modifier = Modifier.padding(horizontal = Banana.Space.xl)
        )
        Spacer(Modifier.height(Banana.Space.m))
        repeat(3) { idx ->
            SkeletonRow()
            if (idx < 2) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp, end = Banana.Space.xl),
                    color = Banana.Color.StrokeSoft,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Banana.Space.xl, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBlock(width = 44.dp, height = 44.dp, radius = Banana.Radius.m)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBlock(width = 140.dp, height = 14.dp)
            ShimmerBlock(width = 80.dp, height = 10.dp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBlock(width = 70.dp, height = 14.dp)
            ShimmerBlock(width = 50.dp, height = 10.dp)
        }
    }
}

@Composable
private fun ShimmerBlock(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(radius))
            .shimmer()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.EmptyState(visible: Boolean) {
    val transition = rememberInfiniteTransition(label = "empty")
    val floatY by transition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420)) +
                slideInVertically(tween(540, easing = Banana.Motion.EaseOut)) { it / 5 },
        modifier = Modifier.align(Alignment.Center)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "🍌",
                fontSize = 64.sp,
                modifier = Modifier.graphicsLayer { translationY = floatY }
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Nothing yet",
                style = Banana.Type.title,
                color = Banana.Color.Ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pay through any UPI app, or tap +\nto add an expense manually.",
                style = Banana.Type.body,
                color = Banana.Color.InkSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
data class DayGroup(val label: String, val total: Double, val items: List<Transaction>)

private fun groupByDay(list: List<Transaction>): List<DayGroup> {
    if (list.isEmpty()) return emptyList()
    val today = java.text.SimpleDateFormat("dd-MM-yy", java.util.Locale.getDefault())
        .format(java.util.Date())
    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val yesterday = java.text.SimpleDateFormat("dd-MM-yy", java.util.Locale.getDefault())
        .format(cal.time)

    return list.groupBy { it.date }.entries
        .map { (date, items) ->
            val label = when (date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> date
            }
            DayGroup(label = label, total = items.sumOf { it.amount }, items = items)
        }
}

internal fun formatMerchant(raw: String): String {
    if (raw.isBlank()) return "—"
    return raw.split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sign-out confirmation dialog — fully designed in the Banana system.
// Spring entrance, two large action buttons. Destructive action uses Error tint.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignOutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Spring-in scale for the dialog body (Apple-style entrance)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = spring(0.7f, Spring.StiffnessMediumLow),
        label = "dialog_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(220, easing = Banana.Motion.EaseOut),
        label = "dialog_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Banana.Space.xxl)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale; this.alpha = alpha
                }
                .clip(RoundedCornerShape(Banana.Radius.xxl))
                .background(Banana.Color.Surface)
                .padding(Banana.Space.xxl)
        ) {
            Text(
                "Sign out?",
                style = Banana.Type.title2,
                color = Banana.Color.Ink
            )
            Spacer(Modifier.height(Banana.Space.xs))
            Text(
                "You'll need to sign in again to view your transactions.",
                style = Banana.Type.body,
                color = Banana.Color.InkSecondary
            )
            Spacer(Modifier.height(Banana.Space.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(Banana.Space.s)) {
                // Cancel — neutral, primary-weight
                Pressable(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    pressedScale = 0.97f
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(Banana.Radius.m))
                            .background(Banana.Color.SurfaceDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cancel",
                            style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                            color = Banana.Color.Ink
                        )
                    }
                }
                // Sign out — destructive, error tint
                Pressable(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    pressedScale = 0.97f
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(Banana.Radius.m))
                            .background(Banana.Color.Error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sign out",
                            style = Banana.Type.callout.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add a transaction optimistically and reconcile with the server.
 *
 *   1. Prepend the new tx to local state immediately. Hero amount, category
 *      bars, and the activity feed update on the next frame — no skeleton,
 *      no entrance replay, no layout flash. Other cards stay put.
 *   2. Fire the actual insert. When the server confirms, refetch and merge:
 *      if the freshly-inserted row hasn't surfaced yet (eventual consistency)
 *      we keep the optimistic copy so it never disappears.
 *   3. If the insert fails, restore the pre-insert snapshot.
 */
/**
 * Robust optimistic insert.
 *
 *   1. Prepend the tx to the local list synchronously — UI is instant, hero
 *      counts up, the new row slides into the activity card. The other cards
 *      stay mounted (no re-stagger, no skeleton).
 *   2. POST to Supabase in the background.
 *   3. On success → silently MERGE the server view back in. We never replace
 *      the list wholesale, because another save could be mid-flight and that
 *      would wipe its optimistic row. Instead: take the server list, then
 *      prepend any local rows the server doesn't yet know about.
 *   4. On failure → KEEP the optimistic copy (do NOT revert). Pull-to-refresh
 *      is the user's authoritative reconciliation point. Reverting on
 *      transient network blips is what made txs "disappear sometimes".
 */
private fun insertOptimistic(
    state: MutableState<List<Transaction>>,
    tx: Transaction
) {
    fun keyOf(t: Transaction): String =
        if (t.upiRef.isNotBlank()) t.upiRef
        else "${t.date}|${t.merchant}|${t.amount}"

    val newKey = keyOf(tx)

    // 1. Optimistic prepend (deduped — guards against double-tap).
    val before = state.value
    if (before.none { keyOf(it) == newKey }) {
        state.value = listOf(tx) + before
    }

    // 2 + 3 + 4. Fire to backend; whatever happens, the row stays visible.
    SupabaseClient.insertTransaction(tx) { ok ->
        if (!ok) {
            android.util.Log.w(
                "BananaTx",
                "Insert failed for ${tx.merchant}; keeping optimistic copy"
            )
            return@insertTransaction
        }
        SupabaseFetcher.fetchTransactions { server ->
            val serverKeys = server.mapTo(HashSet()) { keyOf(it) }
            // Anything currently in our local list that the server hasn't
            // surfaced yet — eventual-consistency lag, or another in-flight
            // optimistic insert — survives at the top of the list.
            val localOnly = state.value.filter { keyOf(it) !in serverKeys }
            state.value = localOnly + server
        }
    }
}

fun merchantEmoji(merchant: String): String {
    val m = merchant.uppercase()
    return when {
        m.contains("ZOMATO") || m.contains("SWIGGY") || m.contains("FOOD") -> "🍕"
        m.contains("OLA") || m.contains("UBER") || m.contains("RAPIDO") -> "🚗"
        m.contains("AMAZON") || m.contains("FLIPKART") || m.contains("MYNTRA") -> "🛍"
        m.contains("ELECTRICITY") || m.contains("BILL") || m.contains("RECHARGE") -> "💡"
        m.contains("NETFLIX") || m.contains("SPOTIFY") || m.contains("PRIME") || m.contains("HOTSTAR") -> "🎵"
        m.contains("PHARMACY") || m.contains("MEDIC") || m.contains("HOSPITAL") || m.contains("APOLLO") -> "💊"
        m.contains("PETROL") || m.contains("FUEL") || m.contains("GAS") -> "⛽"
        m.contains("STARBUCKS") || m.contains("CAFE") || m.contains("COFFEE") -> "☕"
        m.contains("STORE") || m.contains("MART") -> "🛒"
        else -> "💳"
    }
}
