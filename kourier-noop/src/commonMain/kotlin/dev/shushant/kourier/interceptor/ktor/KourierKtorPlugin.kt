package dev.shushant.kourier.interceptor.ktor

import io.ktor.client.plugins.api.createClientPlugin

class KourierKtorPluginConfig

val KourierKtorPlugin = createClientPlugin("KourierKtorPlugin", ::KourierKtorPluginConfig) {
    // No-op for release builds: no listeners attached, zero overhead
}
