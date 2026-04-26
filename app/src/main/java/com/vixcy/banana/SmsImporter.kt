package com.vixcy.banana

import android.content.Context
import android.provider.Telephony
import android.util.Log

object SmsImporter {

    fun importExistingSms(context: Context) {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null, null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        ) ?: return

        var inserted = 0
        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(0) ?: continue
                val body = it.getString(1) ?: continue

                if (NotificationParser.isBankSms(sender, body)) {
                    val tx = NotificationParser.parseFromSms(body) ?: continue
                    SupabaseClient.insertTransaction(tx)
                    inserted++
                    Log.d("BananaTx", "Imported: ₹${tx.amount} | ${tx.merchant}")
                }
            }
        }
        Log.d("BananaTx", "Import complete: $inserted transactions")
    }
}