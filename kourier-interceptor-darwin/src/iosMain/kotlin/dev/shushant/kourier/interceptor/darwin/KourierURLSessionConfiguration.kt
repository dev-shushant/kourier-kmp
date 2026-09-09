package dev.shushant.kourier.interceptor.darwin

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLSessionConfiguration

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
object KourierURLSessionConfiguration {

    /**
     * Registers KourierURLProtocol globally with NSURLProtocol.
     * Note: URLSessions configured with custom configurations must still include KourierURLProtocol
     * in their protocolClasses.
     */
    fun install() {
        // KourierURLProtocol.Companion extends NSURLProtocolMeta which is an ObjCClass
        NSURLProtocol.registerClass(KourierURLProtocol)
    }

    /**
     * Injects KourierURLProtocol as the highest priority protocol in an existing NSURLSessionConfiguration.
     * Use this with custom URLSession instances, Alamofire Session, or Moya providers in Swift/iOS.
     */
    fun enable(configuration: NSURLSessionConfiguration): NSURLSessionConfiguration {
        val currentProtocols = configuration.protocolClasses?.toMutableList() ?: mutableListOf()
        if (!currentProtocols.contains(KourierURLProtocol)) {
            currentProtocols.add(0, KourierURLProtocol)
            configuration.protocolClasses = currentProtocols
        }
        return configuration
    }

    /**
     * Unregisters KourierURLProtocol from global interception.
     */
    fun uninstall() {
        NSURLProtocol.unregisterClass(KourierURLProtocol)
    }
}
