package com.vixcy.banana

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        if (!NotificationParser.isUpiApp(packageName)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        Log.d("BananaTx", "Notification from $packageName | $title | $text")

        val transaction = NotificationParser.parse(title, text) ?: return

        Log.d("BananaTx", "Parsed: ₹${transaction.amount} | ${transaction.merchant}")
        SupabaseClient.insertTransaction(transaction)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}