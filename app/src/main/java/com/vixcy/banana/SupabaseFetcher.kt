package com.vixcy.banana

import okhttp3.*
import org.json.JSONArray
import java.io.IOException

object SupabaseFetcher {

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY
    private val client = OkHttpClient()

    fun fetchTransactions(onResult: (List<Transaction>) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val monthStr = "%02d".format(month)
        val fromDate = "$year-$monthStr-01"

        val token = AuthManager.accessToken ?: SUPABASE_KEY

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/transactions?order=created_at.desc&created_at=gte.$fromDate")
            .get()
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("BananaTx", "Fetch failed: ${e.message}")
                onResult(emptyList())
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val list = mutableListOf<Transaction>()
                try {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Transaction(
                                amount = obj.getDouble("amount"),
                                merchant = obj.getString("merchant"),
                                date = obj.getString("date"),
                                accountLast4 = obj.optString("account_last4", "????"),
                                rawSms = obj.optString("raw_sms", ""),
                                upiRef = obj.optString("upi_ref", "")
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BananaTx", "Parse error: ${e.message}")
                }
                onResult(list)
            }
        })
    }
}