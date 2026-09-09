package dev.shushant.kourier.interceptor.darwin

import platform.Foundation.NSURLSessionConfiguration

object KourierURLSessionConfiguration {
    fun install() {
        // No-op for release builds
    }

    fun enable(configuration: NSURLSessionConfiguration): NSURLSessionConfiguration {
        return configuration
    }

    fun uninstall() {
        // No-op for release builds
    }
}
