package com.vixcy.banana

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val bgColor = Color(0xFFF5F4F0)
    val cardColor = Color.White
    val textPrimary = Color(0xFF111111)
    val textMuted = Color(0xFFBBBBBB)
    val accent = Color(0xFFD85A30)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 8.dp, 24.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Add transaction",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )

            // Amount field
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("₹", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("Amount", color = textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Merchant field
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                TextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = { Text("Merchant / Person name", color = textMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (merchant.isBlank()) return@Button
                    isSaving = true
                    val tx = Transaction(
                        amount = amt,
                        merchant = merchant.uppercase().trim(),
                        date = java.text.SimpleDateFormat("dd-MM-yy", java.util.Locale.getDefault()).format(java.util.Date()),
                        accountLast4 = "Manual",
                        rawSms = "manual_entry",
                        upiRef = "manual_${System.currentTimeMillis()}"
                    )
                    onSave(tx)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                enabled = !isSaving
            ) {
                Text(
                    if (isSaving) "Saving..." else "Save transaction",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}