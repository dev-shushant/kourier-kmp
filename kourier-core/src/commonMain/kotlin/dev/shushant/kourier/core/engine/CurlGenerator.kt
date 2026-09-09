package dev.shushant.kourier.core.engine

import dev.shushant.kourier.core.model.HttpRequest

object CurlGenerator {
    /**
     * Generates an executable shell cURL command for an HttpRequest.
     * Escapes single quotes and special characters safely.
     */
    fun generate(request: HttpRequest): String {
        val builder = StringBuilder()
        builder.append("curl --location --request ").append(request.method).append(" '")
        builder.append(request.url.replace("'", "'\\''"))
        builder.append("'")

        for (header in request.headers) {
            builder.append(" \\\n  --header '")
            builder.append(header.name.replace("'", "'\\''"))
            builder.append(": ")
            builder.append(header.value.replace("'", "'\\''"))
            builder.append("'")
        }

        val body = request.body
        if (!body.isNullOrBlank()) {
            builder.append(" \\\n  --data-raw '")
            // Escape single quotes: ' -> '\''
            builder.append(body.replace("'", "'\\''"))
            builder.append("'")
        }

        return builder.toString()
    }
}
