package com.hiennv.flutter_callkit_incoming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object AppUtils {
    fun getAppIntent(context: Context, action: String? = null, data: Bundle? = null, isFullScreen: Boolean = false): Intent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.cloneFilter()
        intent?.addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.putExtra(FlutterCallkitIncomingPlugin.EXTRA_CALLKIT_CALL_DATA, data)
        intent?.action = action
        return intent
    }

    fun postApiWithoutBody(endpoint: String, userToken: String , isCancel: Boolean = false) {
        val client = OkHttpClient()
        // Create JSON body with the isCancel parameter
        val jsonBody = JSONObject().apply {}
        if (isCancel) {
            jsonBody.put("isCancel", true)
        }

        val requestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            jsonBody.toString()
        )

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $userToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    println("Response: $responseBody")
                } else {
                    println("Request failed with code: ${response.code()}")
                }
            }
        })
    }

}