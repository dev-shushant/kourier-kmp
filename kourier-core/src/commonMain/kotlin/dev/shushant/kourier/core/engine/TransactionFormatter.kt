package dev.shushant.kourier.core.engine

import dev.shushant.kourier.core.model.HttpTransaction

object TransactionFormatter {

    /**
     * Formats a single transaction into a comprehensive human-readable report.
     */
    fun formatSingleTransaction(tx: HttpTransaction): String {
        val sb = StringBuilder()
        sb.append("================================================================================\n")
        sb.append("KOURIER HTTP TRANSACTION REPORT\n")
        sb.append("================================================================================\n\n")

        // ── General Info ─────────────────────────────────────────────────────
        sb.append("Method:       ${tx.request.method}\n")
        sb.append("URL:          ${tx.request.url}\n")
        sb.append("Status:       ${tx.formattedStatus}\n")
        sb.append("Duration:     ${tx.durationMs} ms\n")
        if (tx.protocol != null) {
            sb.append("Protocol:     ${tx.protocol}\n")
        }
        sb.append("TLS / HTTPS:  ${if (tx.request.isSsl) "Yes" else "No"}\n")
        if (tx.remoteAddress != null) {
            sb.append("Remote IP:    ${tx.remoteAddress}\n")
        }
        sb.append("TTFB:         ${tx.timings.timeToFirstByteMs} ms\n")
        sb.append("Req Size:     ${tx.totalBytesSent} bytes\n")
        sb.append("Resp Size:    ${tx.totalBytesReceived} bytes\n")
        sb.append("\n")

        // ── Request Section ──────────────────────────────────────────────────
        sb.append("--- [REQUEST HEADERS] ---\n")
        if (tx.request.headers.isEmpty()) {
            sb.append("(No request headers)\n")
        } else {
            for (header in tx.request.headers) {
                sb.append("${header.name}: ${header.value}\n")
            }
        }
        sb.append("\n")

        sb.append("--- [REQUEST BODY] ---\n")
        val reqBody = tx.request.body
        if (reqBody.isNullOrBlank()) {
            sb.append("(Empty request body)\n")
        } else {
            sb.append(reqBody).append("\n")
        }
        sb.append("\n")

        // ── Response Section ─────────────────────────────────────────────────
        val resp = tx.response
        sb.append("--- [RESPONSE HEADERS] ---\n")
        if (resp == null || resp.headers.isEmpty()) {
            sb.append(if (tx.isPending) "(Awaiting response)\n" else "(No response headers)\n")
        } else {
            for (header in resp.headers) {
                sb.append("${header.name}: ${header.value}\n")
            }
        }
        sb.append("\n")

        sb.append("--- [RESPONSE BODY] ---\n")
        val respBody = resp?.body
        if (respBody.isNullOrBlank()) {
            sb.append(if (tx.isPending) "(Awaiting response)\n" else "(Empty response body)\n")
        } else {
            sb.append(respBody).append("\n")
        }
        sb.append("\n")

        // ── Error Section (if any) ───────────────────────────────────────────
        if (tx.error != null) {
            sb.append("--- [ERROR / EXCEPTION] ---\n")
            sb.append("Class:   ${tx.error.exceptionClass}\n")
            sb.append("Message: ${tx.error.message}\n")
            if (tx.error.stackTrace.isNotEmpty()) {
                sb.append("Stack Trace:\n${tx.error.stackTrace}\n")
            }
            sb.append("\n")
        }

        sb.append("================================================================================\n")
        return sb.toString()
    }

    /**
     * Formats a single transaction into a concise single-block summary for instant messaging.
     */
    fun formatSummary(tx: HttpTransaction): String {
        val resp = tx.response
        val statusText = if (resp != null) "${resp.statusCode} ${resp.message}" else if (tx.error != null) "FAILED (${tx.error.exceptionClass})" else "PENDING"
        return """
            [${tx.request.method}] ${tx.request.url}
            Status: $statusText | Duration: ${tx.durationMs}ms
            Req Size: ${tx.totalBytesSent}B | Resp Size: ${tx.totalBytesReceived}B
        """.trimIndent()
    }
}
