package com.vixcy.banana

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.originatingAddress ?: continue
                val body = message.messageBody ?: continue

                if (NotificationParser.isBankSms(sender, body)) {
                    val tx = NotificationParser.parseFromSms(body) ?: continue
                    Log.d("BananaTx", "SMS TX: ₹${tx.amount} | ${tx.merchant}")
                    SupabaseClient.insertTransaction(tx)
                }
            }
        }
    }
}