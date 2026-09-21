package com.example.ai

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Custom OkHttp logging interceptor that scrubs all PII, patient identifiers, and medical data from AI API logs.
 * Retains strictly metadata (status codes, latency in ms, model identifier, and payload sizes) for performance debugging.
 */
class SafeAiLoggingInterceptor(
    private val logToLogcat: Boolean = true
) : Interceptor {

    companion object {
        private const val TAG = "SafeAiLogger"

        @Volatile
        var lastLoggedEvent: LoggedAiEvent? = null
            private set

        data class LoggedAiEvent(
            val method: String,
            val sanitizedUrl: String,
            val modelIdentifier: String,
            val statusCode: Int,
            val latencyMs: Long,
            val requestPayloadSize: Long,
            val responsePayloadSize: Long,
            val isSuccess: Boolean
        )
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 1. Sanitize URL: Strip API keys, tokens, and any secret query parameters
        val sanitizedUrl = request.url.newBuilder().apply {
            request.url.queryParameterNames.forEach { name ->
                if (name.equals("key", ignoreCase = true) ||
                    name.contains("token", ignoreCase = true) ||
                    name.contains("secret", ignoreCase = true) ||
                    name.contains("auth", ignoreCase = true)
                ) {
                    removeAllQueryParameters(name)
                    addQueryParameter(name, "[REDACTED]")
                }
            }
        }.build().toString()

        // 2. Extract model identifier from URL path (e.g. models/gemini-2.5-flash)
        val modelIdentifier = runCatching {
            val segments = request.url.pathSegments
            val modelIndex = segments.indexOf("models")
            if (modelIndex != -1 && modelIndex + 1 < segments.size) {
                segments[modelIndex + 1].substringBefore(":")
            } else {
                "gemini"
            }
        }.getOrDefault("gemini")

        val requestContentLength = request.body?.contentLength() ?: 0L

        // STRICT PII SCRUBBING:
        // NEVER read or log request.body contents (contains medical images, raw OCR, patient names)
        if (logToLogcat) {
            Log.d(TAG, "--> AI REQUEST: ${request.method} $sanitizedUrl | Model: $modelIdentifier | RequestSize: $requestContentLength bytes")
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val latencyMs = (System.nanoTime() - startTime) / 1_000_000
            if (logToLogcat) {
                Log.e(TAG, "<-- AI REQUEST FAILED | Model: $modelIdentifier | Latency: ${latencyMs}ms | Error: ${e.javaClass.simpleName}")
            }
            lastLoggedEvent = LoggedAiEvent(
                method = request.method,
                sanitizedUrl = sanitizedUrl,
                modelIdentifier = modelIdentifier,
                statusCode = -1,
                latencyMs = latencyMs,
                requestPayloadSize = requestContentLength,
                responsePayloadSize = -1L,
                isSuccess = false
            )
            throw e
        }

        val latencyMs = (System.nanoTime() - startTime) / 1_000_000
        val responseContentLength = response.body?.contentLength() ?: -1L

        // STRICT MEDICAL DATA SCRUBBING:
        // NEVER read or log response.body contents (contains lab metrics, diagnoses, clinician notes)
        if (logToLogcat) {
            Log.d(
                TAG,
                "<-- AI RESPONSE: status=${response.code} success=${response.isSuccessful} | Model: $modelIdentifier | Latency: ${latencyMs}ms | ResponseSize: $responseContentLength bytes"
            )
        }

        lastLoggedEvent = LoggedAiEvent(
            method = request.method,
            sanitizedUrl = sanitizedUrl,
            modelIdentifier = modelIdentifier,
            statusCode = response.code,
            latencyMs = latencyMs,
            requestPayloadSize = requestContentLength,
            responsePayloadSize = responseContentLength,
            isSuccess = response.isSuccessful
        )

        return response
    }
}
