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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val scope = rememberCoroutineScope()
            val stats by KourierTelemetry.stats.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemInDark) }

            // Theme-aware color palette
            val bgColor = if (isDarkTheme) Color(0xFF0D1117) else Color(0xFFF6F8FA)
            val cardBg = if (isDarkTheme) Color(0xFF161B22) else Color(0xFFFFFFFF)
            val cardBorder = if (isDarkTheme) Color(0xFF30363D) else Color(0xFFD0D7DE)
            val textPrimary = if (isDarkTheme) Color(0xFFF0F6FC) else Color(0xFF1F2328)
            val textSecondary = if (isDarkTheme) Color(0xFF8B949E) else Color(0xFF656D76)
            val totalColor = if (isDarkTheme) Color(0xFFF0F6FC) else Color(0xFF1F2328)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header with Title & Theme Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kourier Inspector",
                                style = MaterialTheme.typography.h5,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Shake phone or tap below to launch",
                                style = MaterialTheme.typography.body2,
                                color = textSecondary
                            )
                        }

                        // Theme toggle pill button
                        Box(
                            modifier = Modifier
                                .padding(end = 56.dp)
                                .background(cardBg, RoundedCornerShape(16.dp))
                                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                                .clickable { isDarkTheme = !isDarkTheme }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDarkTheme) "☀️ Day" else "🌙 Night",
                                style = MaterialTheme.typography.caption,
                                color = if (isDarkTheme) Color(0xFFD29922) else Color(0xFF58A6FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats card
                    Card(
                        backgroundColor = cardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp)),
                        elevation = if (isDarkTheme) 0.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatColumn(label = "TOTAL", value = "${stats.totalRequests}", color = totalColor, labelColor = textSecondary)
                            StatColumn(label = "ACTIVE", value = "${stats.activeRequests}", color = if (isDarkTheme) Color(0xFFD29922) else Color(0xFF9A6700), labelColor = textSecondary)
                            StatColumn(label = "ERRORS", value = "${stats.errorCount}", color = if (isDarkTheme) Color(0xFFF85149) else Color(0xFFCF222E), labelColor = textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Launch Button
                    Button(
                        onClick = { Kourier.showUI() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isDarkTheme) Color(0xFF238636) else Color(0xFF1A7F37)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = "LAUNCH KOURIER DEBUGGER", color = Color.White, style = MaterialTheme.typography.button)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Batch Simulator Button (The 1-tap showcase action!)
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                executeGetCall()
                                kotlinx.coroutines.delay(180)
                                executePostCallWithAuth()
                                kotlinx.coroutines.delay(180)
                                execute404Call()
                                kotlinx.coroutines.delay(180)
                                execute500Call()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isDarkTheme) Color(0xFF1F6FEB) else Color(0xFF0969DA)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(text = "⚡ FIRE DEMO TRAFFIC BATCH", color = Color.White, style = MaterialTheme.typography.button)
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // ── Section 1: Security & Data Masking ──────────────────
                    SectionHeader(
                        title = "🔒 DATA MASKING & SECURITY",
                        subtitle = "Auto-redacts passwords, tokens, cookies & PII",
                        textColor = textPrimary,
                        subColor = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "POST /auth/login (Redacts Passwords & PII)",
                        engineTag = "OkHttp",
                        color = if (isDarkTheme) Color(0xFFBC8CFF) else Color(0xFF8250DF),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executePostCallWithAuth() }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ── Section 2: Multiplatform Network Traffic ────────────
                    SectionHeader(
                        title = "🌐 MULTIPLATFORM NETWORK ENGINES",
                        subtitle = "Unified interception across OkHttp and Ktor 3",
                        textColor = textPrimary,
                        subColor = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /users (200 OK Standard Request)",
                        engineTag = "OkHttp",
                        color = if (isDarkTheme) Color(0xFF58A6FF) else Color(0xFF0969DA),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executeGetCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "Ktor 3.x Client (GET & POST Pipeline)",
                        engineTag = "Ktor 3",
                        color = if (isDarkTheme) Color(0xFF79C0FF) else Color(0xFF0550AE),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executeKtorCalls() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /image.png (Binary Media Preview)",
                        engineTag = "OkHttp",
                        color = if (isDarkTheme) Color(0xFF2EA043) else Color(0xFF116329),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executeImageDownloadCall() }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ── Section 3: Diagnostics & Error Handling ─────────────
                    SectionHeader(
                        title = "⚠️ ERROR DIAGNOSTICS & RESILIENCE",
                        subtitle = "Real-time error badges, alert trays & latency logs",
                        textColor = textPrimary,
                        subColor = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /status/404 (Client Error)",
                        engineTag = "404",
                        color = if (isDarkTheme) Color(0xFFF85149) else Color(0xFFCF222E),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { execute404Call() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /status/500 (Server Crash Alert)",
                        engineTag = "500",
                        color = if (isDarkTheme) Color(0xFFDA3633) else Color(0xFFA40E26),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { execute500Call() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /delay/3s (Latency & Waterfall)",
                        engineTag = "3s",
                        color = if (isDarkTheme) Color(0xFFD29922) else Color(0xFF9A6700),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executeSlowCall() }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ScenarioButton(
                        title = "GET /bytes/1MB (Large Payload Truncation)",
                        engineTag = "1MB",
                        color = if (isDarkTheme) Color(0xFF3FB950) else Color(0xFF1A7F37),
                        isDark = isDarkTheme
                    ) {
                        scope.launch(Dispatchers.IO) { executeLargePayloadCall() }
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
private fun StatColumn(
    label: String,
    value: String,
    color: Color,
    labelColor: Color = Color.Gray
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.h6, color = color)
        Text(text = label, style = MaterialTheme.typography.caption, color = labelColor)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    textColor: Color,
    subColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = textColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.caption,
            color = subColor
        )
    }
}

@Composable
private fun ScenarioButton(
    title: String,
    engineTag: String = "",
    color: Color,
    isDark: Boolean = true,
    onClick: () -> Unit
) {
    androidx.compose.material.Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isDark) color.copy(alpha = 0.12f) else color.copy(alpha = 0.08f)
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(
                1.dp,
                if (isDark) color.copy(alpha = 0.5f) else color.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = color,
                style = MaterialTheme.typography.button,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (engineTag.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = if (isDark) 0.25f else 0.18f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = engineTag,
                        style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                        color = color
                    )
                }
            }
        }
    }
}


