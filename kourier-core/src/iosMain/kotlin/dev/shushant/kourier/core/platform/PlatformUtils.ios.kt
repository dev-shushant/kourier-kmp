package dev.shushant.kourier.core.platform

import dev.shushant.kourier.core.model.CallStackElement
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
actual object PlatformUtils {
    actual fun randomUuid(): String = NSUUID().UUIDString

    actual fun nanoTime(): Long {
        return (NSDate().timeIntervalSince1970 * 1_000_000_000.0).toLong()
    }

    actual fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
    }

    actual fun platformName(): String {
        val device = UIDevice.currentDevice
        return "${device.systemName} ${device.systemVersion} (${device.model})"
    }

    actual fun captureSanitizedCallStack(maxDepth: Int): List<CallStackElement> {
        val symbols = NSThread.callStackSymbols
        val count = symbols.size
        if (count == 0) return emptyList()

        val result = mutableListOf<CallStackElement>()

        for (i in 0 until count) {
            val item = symbols[i]
            val line = item?.toString() ?: continue
            // Ignore Kourier internal frames
            if (line.contains("dev.shushant.kourier") || line.contains("Kourier") || line.contains("PlatformUtils")) {
                continue
            }

            // Typical iOS symbol: "1  MyApp  0x0000000100003f50 +[MyClass myMethod] + 48"
            val parts = line.split("\\s+".toRegex())
            val binary = if (parts.size > 1) parts[1] else "Native"
            val symbol = if (parts.size > 3) parts.drop(3).joinToString(" ") else line

            result.add(
                CallStackElement(
                    className = binary,
                    methodName = symbol,
                    fileName = null,
                    lineNumber = -1,
                    isAppCode = !binary.startsWith("libsystem") && !binary.startsWith("CFNetwork")
                )
            )

            if (result.size >= maxDepth) break
        }

        return result
    }
}

