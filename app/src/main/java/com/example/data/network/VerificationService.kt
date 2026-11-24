package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RealDebridVerifyResult(
    val success: Boolean,
    val userLabel: String = "",
    val premiumDays: Long = 0L,
    val message: String,
)

data class ApiVerifyResult(
    val success: Boolean,
    val message: String,
)

class VerificationService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Live verification against Real-Debrid's official REST API:
     * GET https://api.real-debrid.com/rest/1.0/user
     */
    suspend fun verifyRealDebridKey(apiKey: String): RealDebridVerifyResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext RealDebridVerifyResult(
                success = false,
                message = "Enter your Real-Debrid Secret API Key first.",
            )
        }
        try {
            val request = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/user")
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    val username = json.optString("username", "Real-Debrid User")
                    val accountType = json.optString("type", "free")
                    val premiumSeconds = json.optLong("premium", 0L)
                    val days = (premiumSeconds / 86_400L).coerceAtLeast(0L)
                    val label = "$username ($accountType)"
                    val detail = if (accountType.equals("premium", ignoreCase = true)) {
                        "Verified: $username • Premium active ($days days left)"
                    } else {
                        "Verified: $username • Free tier (upgrade to Premium for cached streams)"
                    }
                    RealDebridVerifyResult(
                        success = true,
                        userLabel = label,
                        premiumDays = days,
                        message = detail,
                    )
                } else if (response.code == 401 || response.code == 403) {
                    RealDebridVerifyResult(
                        success = false,
                        message = "Invalid or expired Real-Debrid API key (HTTP ${response.code}). Copy it fresh from real-debrid.com/apitoken.",
                    )
                } else {
                    RealDebridVerifyResult(
                        success = false,
                        message = "Real-Debrid returned HTTP ${response.code}. Please try again.",
                    )
                }
            }
        } catch (e: Exception) {
            RealDebridVerifyResult(
                success = false,
                message = "Network error reaching Real-Debrid: ${e.localizedMessage ?: "Check connection"}",
            )
        }
    }

    /**
     * Live verification against TMDB v3/v4 authentication endpoint.
     */
    suspend fun verifyTmdbKey(apiKey: String): ApiVerifyResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext ApiVerifyResult(false, "Enter your TMDB API key or Read Access Token first.")
        }
        try {
            val requestBuilder = if (cleanKey.length > 45 && cleanKey.contains(".")) {
                // v4 Read Access Token (JWT)
                Request.Builder()
                    .url("https://api.themoviedb.org/3/authentication")
                    .header("Authorization", "Bearer $cleanKey")
                    .header("accept", "application/json")
            } else {
                // v3 API Key
                Request.Builder()
                    .url("https://api.themoviedb.org/3/authentication?api_key=$cleanKey")
                    .header("accept", "application/json")
            }

            client.newCall(requestBuilder.get().build()).execute().use { response ->
                if (response.isSuccessful) {
                    ApiVerifyResult(true, "Verified TMDB API key! Ready for AIOMetadata.")
                } else if (response.code == 401) {
                    ApiVerifyResult(false, "TMDB rejected this key (HTTP 401). Double-check for trailing spaces.")
                } else {
                    ApiVerifyResult(false, "TMDB check failed (HTTP ${response.code}).")
                }
            }
        } catch (e: Exception) {
            ApiVerifyResult(false, "Network error reaching TMDB: ${e.localizedMessage ?: "Check connection"}")
        }
    }

    /**
     * Live HTTP inspection of a Stremio/Nuvio addon manifest.json URL (AIOStreams or AIOMetadata).
     */
    suspend fun verifyAddonManifest(rawUrl: String): ApiVerifyResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return@withContext ApiVerifyResult(false, "Paste your generated manifest.json URL first.")
        }
        val normalizedUrl = when {
            trimmed.startsWith("stremio://", ignoreCase = true) ->
                "https://" + trimmed.removePrefix("stremio://").removePrefix("STREMIO://")
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) ->
                trimmed
            else -> "https://$trimmed"
        }

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyStr.trimStart().startsWith("{")) {
                    val json = JSONObject(bodyStr)
                    val addonName = json.optString("name", "")
                    val version = json.optString("version", "")
                    val catalogCount = json.optJSONArray("catalogs")?.length() ?: 0
                    val resourceCount = json.optJSONArray("resources")?.length() ?: 0
                    if (addonName.isNotBlank()) {
                        val summary = buildString {
                            append("Live Manifest Verified: $addonName")
                            if (version.isNotBlank()) append(" v$version")
                            if (catalogCount > 0) append(" • $catalogCount catalogs")
                            if (resourceCount > 0) append(" • $resourceCount resources")
                        }
                        ApiVerifyResult(true, summary)
                    } else {
                        ApiVerifyResult(false, "URL returned JSON, but no addon 'name' field was found. Make sure it ends with /manifest.json.")
                    }
                } else {
                    ApiVerifyResult(false, "Manifest endpoint returned HTTP ${response.code}. Ensure you copied the full /manifest.json link.")
                }
            }
        } catch (e: Exception) {
            ApiVerifyResult(false, "Could not reach manifest URL: ${e.localizedMessage ?: "Invalid URL"}")
        }
    }
}
