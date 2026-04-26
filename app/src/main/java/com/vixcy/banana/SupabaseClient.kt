package com.vixcy.banana

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object SupabaseClient {

    private const val SUPABASE_URL = "https://qjzgjtgghlhhitgokubr.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqemdqdGdnaGxoaGl0Z29rdWJyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcwNDM1ODYsImV4cCI6MjA5MjYxOTU4Nn0.CMQReI9IGNY17LeU7js86UfmpSzGuUMW8fMp3cmemZs"

    private val client = OkHttpClient()

    fun insertTransaction(transaction: Transaction) {
        val json = JSONObject().apply {
            put("amount", transaction.amount)
            put("merchant", transaction.merchant)
            put("date", transaction.date)
            put("account_last4", transaction.accountLast4)
            put("raw_sms", transaction.rawSms)
            put("upi_ref", transaction.upiRef)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/transactions")
            .post(body)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("BananaTx", "Insert failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("BananaTx", "Inserted: ${response.code}")
            }
        })
    }
}