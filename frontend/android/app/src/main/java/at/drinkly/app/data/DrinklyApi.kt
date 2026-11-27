package at.drinkly.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime

/** Talks to the Quarkus backend (`/entries` and the `/events` stream). */
class DrinklyApi(private val baseUrl: () -> String) {

    sealed interface LiveEvent {
        data object Connected : LiveEvent
        data object Changed : LiveEvent
        data object Disconnected : LiveEvent
    }

    suspend fun entries(username: String): List<DrinkEntry> = withContext(Dispatchers.IO) {
        val array = JSONArray(request("GET", "/entries/" + encode(username)))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            DrinkEntry(
                id = o.getInt("id"),
                timestamp = o.optString("timestamp").takeIf { it.isNotEmpty() && it != "null" }
                    ?.let { OffsetDateTime.parse(it).toInstant() } ?: Instant.now(),
                liters = o.getDouble("drinkingAmount"),
            )
        }
    }

    suspend fun add(username: String, milliliters: Int) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("user", JSONObject().put("username", username))
            .put("drinkingAmount", milliliters / 1000.0)
        request("POST", "/entries", body.toString())
        Unit
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        request("DELETE", "/entries/$id")
        Unit
    }

    /**
     * Server-sent events: the backend says "connected" when the stream opens and
     * "changed" whenever an entry is added or removed. Reconnects on its own.
     */
    fun liveEvents(): Flow<LiveEvent> = flow {
        while (true) {
            try {
                val conn = open("GET", "/events").apply {
                    readTimeout = 0
                    setRequestProperty("Accept", "text/event-stream")
                }
                conn.inputStream.bufferedReader().use { reader ->
                    emit(LiveEvent.Connected)
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("data:") && line.substringAfter("data:").trim() == "changed") {
                            emit(LiveEvent.Changed)
                        }
                    }
                }
            } catch (_: IOException) {
            }
            emit(LiveEvent.Disconnected)
            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    private fun request(method: String, path: String, body: String? = null): String {
        val conn = open(method, path)
        try {
            // Always JSON: without it Android sends a form type on DELETE and the backend answers 415
            conn.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val message = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                throw IOException(message.ifBlank { "Server answered $code" })
            }
            return if (code == 204) "" else conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun open(method: String, path: String): HttpURLConnection {
        val conn = URL(baseUrl().trimEnd('/') + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 10000
        return conn
    }

    private fun encode(segment: String) = URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
}
