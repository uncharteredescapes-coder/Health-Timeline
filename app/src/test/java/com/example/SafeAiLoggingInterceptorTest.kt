package com.example

import com.example.ai.SafeAiLoggingInterceptor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class SafeAiLoggingInterceptorTest {

    @Test
    fun testSecretAndKeyRedactionInUrl() {
        val interceptor = SafeAiLoggingInterceptor(logToLogcat = false)

        val originalRequest = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSyDSECRETKEY123&authToken=SECRET_TOKEN")
            .post("{\"patientName\":\"John Doe\",\"hba1c\":7.2}".toRequestBody("application/json".toMediaType()))
            .build()

        val mockChain = object : Interceptor.Chain {
            override fun request(): Request = originalRequest
            override fun proceed(request: Request): Response {
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"results\":[{\"metric\":\"glucose\",\"value\":110}]}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        }

        val response = interceptor.intercept(mockChain)
        assertEquals(200, response.code)

        val loggedEvent = SafeAiLoggingInterceptor.lastLoggedEvent
        assertNotNull(loggedEvent)
        assertEquals("POST", loggedEvent?.method)
        assertEquals(200, loggedEvent?.statusCode)
        assertEquals("gemini-2.5-flash", loggedEvent?.modelIdentifier)
        assertTrue(loggedEvent?.isSuccess == true)

        // Verify API secret key was redacted
        assertFalse("Logged URL must NOT contain the secret API key", loggedEvent!!.sanitizedUrl.contains("AIzaSyDSECRETKEY123"))
        assertFalse("Logged URL must NOT contain the secret token", loggedEvent.sanitizedUrl.contains("SECRET_TOKEN"))
        assertTrue("Logged URL must have key redacted", loggedEvent.sanitizedUrl.contains("key=%5BREDACTED%5D") || loggedEvent.sanitizedUrl.contains("key=[REDACTED]"))

        // Verify patient name and clinical results are NEVER part of the logged event
        assertFalse("Logged event must not contain patient name", loggedEvent.toString().contains("John Doe"))
        assertFalse("Logged event must not contain clinical results", loggedEvent.toString().contains("glucose"))
    }
}
