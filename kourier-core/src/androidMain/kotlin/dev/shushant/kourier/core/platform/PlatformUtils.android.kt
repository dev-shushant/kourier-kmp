package dev.shushant.kourier.core.platform

import android.os.Build
import dev.shushant.kourier.core.model.CallStackElement
import java.util.UUID

actual object PlatformUtils {
    actual fun randomUuid(): String = UUID.randomUUID().toString()

    actual fun nanoTime(): Long = System.nanoTime()

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun platformName(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    private val EXCLUDED_PACKAGES = listOf(
        "dev.shushant.kourier",
        "okhttp3",
        "okio",
        "io.ktor",
        "kotlinx.coroutines",
        "java.lang.Thread",
        "dalvik.system.VMStack"
    )

    actual fun captureSanitizedCallStack(maxDepth: Int): List<CallStackElement> {
        val stackTrace = Thread.currentThread().stackTrace
        val result = mutableListOf<CallStackElement>()

        for (element in stackTrace) {
            val className = element.className
            val isExcluded = EXCLUDED_PACKAGES.any { className.startsWith(it) }
            if (isExcluded) continue

            val isAppCode = !className.startsWith("android.") &&
                    !className.startsWith("androidx.") &&
                    !className.startsWith("java.") &&
                    !className.startsWith("javax.") &&
                    !className.startsWith("kotlin.")

            result.add(
                CallStackElement(
                    className = element.className,
                    methodName = element.methodName,
                    fileName = element.fileName,
                    lineNumber = element.lineNumber,
                    isAppCode = isAppCode
                )
            )

            if (result.size >= maxDepth) break
        }

        return result
    }
}
