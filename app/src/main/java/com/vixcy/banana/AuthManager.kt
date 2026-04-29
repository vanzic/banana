package com.vixcy.banana

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object AuthManager {

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY
    private val client = OkHttpClient()

    private var _accessToken: String? = null
    private var _userId: String? = null

    val accessToken get() = _accessToken
    val userId get() = _userId
    val isLoggedIn get() = _accessToken != null

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/signup")
            .post(body)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    _accessToken = json.optString("access_token")
                    _userId = json.optJSONObject("user")?.optString("id")
                    onResult(true, null)
                } else {
                    val error = JSONObject(bodyStr).optString("msg", "Signup failed")
                    onResult(false, error)
                }
            }
        })
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
            .post(body)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    _accessToken = json.optString("access_token")
                    _userId = json.optJSONObject("user")?.optString("id")
                    onResult(true, null)
                } else {
                    val error = JSONObject(bodyStr).optString("msg", "Login failed")
                    onResult(false, error)
                }
            }
        })
    }

    fun signOut() {
        _accessToken = null
        _userId = null
    }
}