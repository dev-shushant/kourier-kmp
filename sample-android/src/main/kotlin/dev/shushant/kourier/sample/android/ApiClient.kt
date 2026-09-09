package dev.shushant.kourier.sample.android

import dev.shushant.kourier.interceptor.ktor.KourierKtorPlugin
import dev.shushant.kourier.interceptor.okhttp.KourierOkHttpInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * Standard OkHttpClient configured with KourierOkHttpInterceptor.
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(KourierOkHttpInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Cross-platform Ktor HttpClient configured with KourierKtorPlugin.
     * Note: Does NOT install KourierOkHttpInterceptor at the OkHttp engine level
     * to prevent double-interception.
     */
    val ktorClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(KourierKtorPlugin)
        }
    }
}
