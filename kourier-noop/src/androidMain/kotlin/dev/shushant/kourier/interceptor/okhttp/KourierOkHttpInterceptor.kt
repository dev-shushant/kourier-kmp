package dev.shushant.kourier.interceptor.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class KourierOkHttpInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // Direct pass-through with zero inspection overhead
        return chain.proceed(chain.request())
    }
}
