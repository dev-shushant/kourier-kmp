package dev.shushant.kourier.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.android.Kourier
import dev.shushant.kourier.android.KourierTelemetry
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import android.content.Intent
import kotlinx.coroutines.CoroutineScope

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val scope = rememberCoroutineScope()
            val stats by KourierTelemetry.stats.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Kourier Inspector Sample",
                        style = MaterialTheme.typography.h5,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Shake phone or tap below to launch debugger",
                        style = MaterialTheme.typography.body2,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats card
                    Card(
                        backgroundColor = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatColumn(label = "TOTAL", value = "${stats.totalRequests}", color = Color.White)
                            StatColumn(label = "ACTIVE", value = "${stats.activeRequests}", color = Color(0xFFD29922))
                            StatColumn(label = "ERRORS", value = "${stats.errorCount}", color = Color(0xFFF85149))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Launch Kourier Button
                    Button(
                        onClick = { Kourier.showUI() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = "LAUNCH KOURIER DEBUGGER", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scenario Matrix
                    ScenarioButton(
                        title = "1. GET 200 OK (OkHttp)",
                        color = Color(0xFF58A6FF)
                    ) {
                        scope.launch(Dispatchers.IO) { executeGetCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "2. POST Masked Auth (OkHttp)",
                        color = Color(0xFFBC8CFF)
                    ) {
                        scope.launch(Dispatchers.IO) { executePostCallWithAuth() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "3. GET 404 Client Error (OkHttp)",
                        color = Color(0xFFF85149)
                    ) {
                        scope.launch(Dispatchers.IO) { execute404Call() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "4. GET 500 Server Error (OkHttp)",
                        color = Color(0xFFDA3633)
                    ) {
                        scope.launch(Dispatchers.IO) { execute500Call() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "5. Slow Call 3s Delay (OkHttp)",
                        color = Color(0xFFD29922)
                    ) {
                        scope.launch(Dispatchers.IO) { executeSlowCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "6. Large Payload >1MB (OkHttp)",
                        color = Color(0xFF3FB950)
                    ) {
                        scope.launch(Dispatchers.IO) { executeLargePayloadCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "7. Image Download (OkHttp)",
                        color = Color(0xFF2EA043)
                    ) {
                        scope.launch(Dispatchers.IO) { executeImageDownloadCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "8. Ktor Client GET & POST",
                        color = Color(0xFF79C0FF)
                    ) {
                        scope.launch(Dispatchers.IO) { executeKtorCalls() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val triggerAll = intent.getBooleanExtra("TRIGGER_NETWORK_CALLS", false)
        val triggerGet = intent.getBooleanExtra("TRIGGER_GET", false)
        val trigger500 = intent.getBooleanExtra("TRIGGER_500", false)
        CoroutineScope(Dispatchers.IO).launch {
            if (triggerAll) {
                executeGetCall()
                executePostCallWithAuth()
                execute500Call()
            }
            if (triggerGet) {
                executeGetCall()
            }
            if (trigger500) {
                execute500Call()
            }
        }
    }

    private fun executeGetCall() {
        val request = Request.Builder()
            .url("https://httpbin.org/get?query=kourier_sample&timestamp=${System.currentTimeMillis()}")
            .header("User-Agent", "KourierSample/1.0")
            .header("Accept", "application/json")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun executePostCallWithAuth() {
        val jsonPayload = """
            {
                "username": "developer@example.com",
                "password": "SuperSecretPassword99!",
                "token": "tok_live_1234567890",
                "credit_card": "4111-2222-3333-4444",
                "ssn": "123-45-6789"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://httpbin.org/post")
            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            .header("Cookie", "session_id=abcdef1234567890")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun execute404Call() {
        val request = Request.Builder()
            .url("https://httpbin.org/status/404")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun execute500Call() {
        val request = Request.Builder()
            .url("https://httpbin.org/status/500")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun executeSlowCall() {
        val request = Request.Builder()
            .url("https://httpbin.org/delay/3")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun executeLargePayloadCall() {
        val request = Request.Builder()
            .url("https://httpbin.org/bytes/1048576")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun executeImageDownloadCall() {
        val request = Request.Builder()
            .url("https://httpbin.org/image/png")
            .build()

        try {
            ApiClient.okHttpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private suspend fun executeKtorCalls() {
        try {
            ApiClient.ktorClient.get("https://httpbin.org/get?source=ktor_client") {
                header("X-Client", "Ktor 3.1.3")
            }
            ApiClient.ktorClient.post("https://httpbin.org/post") {
                header("Content-Type", "application/json")
                setBody("""{"service":"kourier-ktor","version":"1.0.0"}""")
            }
        } catch (_: Exception) {}
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.h6, color = color)
        Text(text = label, style = MaterialTheme.typography.caption, color = Color.Gray)
    }
}

@Composable
private fun ScenarioButton(title: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(42.dp)
    ) {
        Text(text = title, color = color)
    }
}
