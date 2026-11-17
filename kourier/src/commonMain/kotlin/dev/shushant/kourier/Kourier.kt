package dev.shushant.kourier

/**
 * Common entry point for KMP consumers.
 *
 * Initialisation is platform-specific because Android needs a Context:
 *  - Android: `Kourier.init(context) { ... }`
 *  - iOS:     `Kourier.initialize { ... }`
 *
 * Wrap those two calls in your own expect/actual (or platform source set), then use the
 * common functions below from shared code.
 */
expect object Kourier {
    /** Opens the Kourier inspector UI. */
    fun showUI()

    /** Closes the Kourier inspector UI. */
    fun hideUI()

    /**
     * Subscribes to live telemetry. Returns a lambda that cancels the subscription.
     */
    fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit
}
