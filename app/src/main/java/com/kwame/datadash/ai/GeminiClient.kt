package com.kwame.datadash.ai

import com.kwame.datadash.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Thin client for the Gemini API (generateContent endpoint), using only
 * Android's built-in HttpURLConnection and org.json — no extra network
 * or JSON library needed.
 *
 * NOTE: this currently sends data straight to Gemini's free tier. Per
 * the project plan, once DataDash has real paying clients, this must
 * be gated so real client data never goes through the free tier—add
 * a dev/prod flag and masking step here before that point.
 */
object GeminiClient {

    private const val MODEL = "gemini-3.8-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun generate(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("No Gemini API key configured for this build")
                )
            }

            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.doOutput = true
            connection.connectTimeout = 20000
            connection.readTimeout = 30000

            val requestBody = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )
            }

            connection.outputStream.use {
                it.write(requestBody.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream.bufferedReader().use { it.readText() }

            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("Gemini API error ($responseCode): $responseText"))
            }

            val text = JSONObject(responseText)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
