package com.vixcy.banana

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vixcy.banana.ui.theme.BananaTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        requestSmsPermission()

        Thread {
            SmsImporter.importExistingSms(this)
        }.start()

        setContent {
            BananaTheme {
                DashboardScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        val enabled = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
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

@Composable
fun DashboardScreen() {
    val transactions = remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }

    fun loadTransactions() {
        SupabaseFetcher.fetchTransactions { result ->
            transactions.value = result
            isLoading.value = false
        }
    }

    LaunchedEffect(Unit) {
        loadTransactions()
    }

    val totalSpent = transactions.value.sumOf { it.amount }
    val categoryTotals = transactions.value
        .groupBy { NotificationParser.categorize(it.merchant) }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
    val maxCat = categoryTotals.firstOrNull()?.value ?: 1.0

    val bgColor = Color(0xFFF5F4F0)
    val cardColor = Color.White
    val textPrimary = Color(0xFF111111)
    val textSecondary = Color(0xFF888888)
    val textMuted = Color(0xFFBBBBBB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (isLoading.value) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = textSecondary
            )
        } else if (transactions.value.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🍌", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No transactions yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Tap + to add one manually",
                    fontSize = 13.sp,
                    color = Color(0xFFBBBBBB)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 16.dp, 16.dp, 0.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Varun's Wallet",
                                fontSize = 13.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color(0xFFF0EFEB)
                            ) {
                                Text(
                                    text = monthName,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("TOTAL SPENT", fontSize = 11.sp, color = textMuted, letterSpacing = 0.08.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "₹${"%,.0f".format(totalSpent)}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF0EFEB), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${transactions.value.size} transactions", fontSize = 12.sp, color = textMuted)
                            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFAECE7)) {
                                Text(
                                    "this month",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFD85A30),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stat Cards
                val biggest = transactions.value.maxByOrNull { it.amount }
                val mostFrequent = transactions.value
                    .groupingBy { it.merchant }
                    .eachCount()
                    .maxByOrNull { it.value }

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🔥",
                        label = "BIGGEST",
                        value = "₹${"%,.0f".format(biggest?.amount ?: 0.0)}",
                        sub = biggest?.merchant ?: "-",
                        bgColor = cardColor
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = "⚡",
                        label = "MOST VISITS",
                        value = mostFrequent?.key ?: "-",
                        sub = "${mostFrequent?.value ?: 0} times",
                        bgColor = cardColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Breakdown
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Spending breakdown",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val barColors = listOf(
                            Color(0xFFD85A30), Color(0xFF7F77DD),
                            Color(0xFF378ADD), Color(0xFF639922), Color(0xFF888780)
                        )
                        categoryTotals.take(5).forEachIndexed { index, entry ->
                            CategoryBar(
                                name = entry.key,
                                amount = entry.value,
                                fraction = (entry.value / maxCat).toFloat(),
                                color = barColors[index % barColors.size],
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )
                            if (index < categoryTotals.take(5).size - 1)
                                Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recent Transactions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp, 18.dp, 18.dp, 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Recent",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                            Text("See all", fontSize = 12.sp, color = textMuted)
                        }
                        transactions.value.take(10).forEachIndexed { index, tx ->
                            TransactionRow(tx = tx, textPrimary = textPrimary, textMuted = textMuted)
                            if (index < transactions.value.take(10).size - 1)
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    color = Color(0xFFF5F4F0),
                                    thickness = 0.5.dp
                                )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF111111),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Light)
        }

        // Manual Entry Sheet
        if (showSheet) {
            ManualEntrySheet(
                onDismiss = { showSheet = false },
                onSave = { tx ->
                    SupabaseClient.insertTransaction(tx)
                    showSheet = false
                    isLoading.value = true
                    SupabaseFetcher.fetchTransactions { result ->
                        transactions.value = result
                        isLoading.value = false
                    }
                }
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: String, label: String, value: String, sub: String, bgColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFFAAAAAA), letterSpacing = 0.04.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111111), letterSpacing = (-0.5).sp)
            Text(sub, fontSize = 11.sp, color = Color(0xFFBBBBBB))
        }
    }
}

@Composable
fun CategoryBar(name: String, amount: Double, fraction: Float, color: Color, textPrimary: Color, textMuted: Color) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            Text("₹${"%,.0f".format(amount)}", fontSize = 13.sp, color = textMuted)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFFF5F4F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, textPrimary: Color, textMuted: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFFFAECE7)),
            contentAlignment = Alignment.Center
        ) {
            Text(merchantEmoji(tx.merchant), fontSize = 17.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.merchant, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            Text(tx.date, fontSize = 11.sp, color = textMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("−₹${"%,.0f".format(tx.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            Text("••${tx.accountLast4}", fontSize = 11.sp, color = Color(0xFFCCCCCC))
        }
    }
}

fun merchantEmoji(merchant: String): String {
    val m = merchant.uppercase()
    return when {
        m.contains("ZOMATO") || m.contains("SWIGGY") -> "🍕"
        m.contains("OLA") || m.contains("UBER") || m.contains("RAPIDO") -> "🚗"
        m.contains("AMAZON") || m.contains("FLIPKART") || m.contains("MYNTRA") -> "🛍"
        m.contains("ELECTRICITY") || m.contains("BILL") -> "💡"
        m.contains("NETFLIX") || m.contains("SPOTIFY") || m.contains("PRIME") -> "🎵"
        m.contains("PHARMACY") || m.contains("MEDIC") || m.contains("HOSPITAL") -> "💊"
        else -> "💳"
    }
}