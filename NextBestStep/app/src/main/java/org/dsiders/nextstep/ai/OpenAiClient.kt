package org.dsiders.nextstep.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dsiders.nextstep.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thin wrapper over the OpenAI Chat Completions API in JSON mode.
 *
 * The language model only ever turns text into fields or words a question. Every
 * decision about what a student has or is missing is made by the engine, which
 * falls back to offline templates whenever this client throws.
 */
class OpenAiClient(
    private val apiKey: String = BuildConfig.OPENAI_API_KEY,
    private val model: String = BuildConfig.OPENAI_MODEL,
    private val baseUrl: String = BuildConfig.OPENAI_BASE_URL,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = apiKey.isNotBlank()
    val modelName: String get() = model

    /**
     * Sends a system + user prompt and returns the parsed JSON object reply.
     * [imagesBase64Jpeg] are attached to the user message (used for resume pages).
     */
    suspend fun json(
        system: String,
        user: String,
        imagesBase64Jpeg: List<String> = emptyList(),
    ): JSONObject = withContext(Dispatchers.IO) {
        if (!isConfigured) throw AiUnavailableException("No OpenAI API key set in local.properties")

        val userContent: Any = if (imagesBase64Jpeg.isEmpty()) user else JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", user))
            imagesBase64Jpeg.forEach { b64 ->
                put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$b64").put("detail", "high"),
                    )
                )
            }
        }

        val body = JSONObject()
            .put("model", model)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
                        .getOrDefault("HTTP ${response.code}")
                    throw AiUnavailableException("OpenAI error: $msg")
                }
                val content = JSONObject(text)
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content")
                JSONObject(content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))
            }
        } catch (e: AiUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw AiUnavailableException("Could not reach OpenAI: ${e.message}", e)
        }
    }
}
