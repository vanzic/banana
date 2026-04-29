package com.vixcy.banana

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object SupabaseClient {

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY
    private val client = OkHttpClient()

    fun insertTransaction(
        transaction: Transaction,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val json = JSONObject().apply {
            put("amount", transaction.amount)
            put("merchant", transaction.merchant)
            put("date", transaction.date)
            put("account_last4", transaction.accountLast4)
            put("raw_sms", transaction.rawSms)
            put("upi_ref", transaction.upiRef)
            AuthManager.userId?.let { put("user_id", it) }
        }

        val token = AuthManager.accessToken ?: SUPABASE_KEY
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/transactions")
            .post(body)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("BananaTx", "Insert failed: ${e.message}")
                onComplete(false)
            }
            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("BananaTx", "Inserted: ${response.code}")
                onComplete(response.isSuccessful)
            }
        })
    }

    /**
     * Delete a transaction. Matches by upi_ref when available (which is unique
     * for both real UPI refs and our `manual_<timestamp>` IDs); falls back to
     * a (amount + merchant + date) composite match for legacy SMS rows that
     * may have an empty upi_ref.
     */
    fun deleteTransaction(transaction: Transaction, callback: (Boolean) -> Unit = {}) {
        val token = AuthManager.accessToken ?: SUPABASE_KEY
        val urlBuilder = "$SUPABASE_URL/rest/v1/transactions".toHttpUrl().newBuilder()

        if (transaction.upiRef.isNotBlank()) {
            urlBuilder.addQueryParameter("upi_ref", "eq.${transaction.upiRef}")
        } else {
            urlBuilder
                .addQueryParameter("amount", "eq.${transaction.amount}")
                .addQueryParameter("merchant", "eq.${transaction.merchant}")
                .addQueryParameter("date", "eq.${transaction.date}")
        }
        AuthManager.userId?.let { urlBuilder.addQueryParameter("user_id", "eq.$it") }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .delete()
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "return=minimal")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("BananaTx", "Delete failed: ${e.message}")
                callback(false)
            }
            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("BananaTx", "Deleted: ${response.code}")
                callback(response.isSuccessful)
            }
        })
    }
}