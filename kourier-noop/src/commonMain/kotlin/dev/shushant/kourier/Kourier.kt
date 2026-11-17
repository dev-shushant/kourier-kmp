package dev.shushant.kourier

/** No-op mirror of the KMP umbrella entry point. Zero overhead in release builds. */
expect object Kourier {
    fun showUI()
    fun hideUI()
    fun observeStats(callback: (total: Long, active: Int, errors: Long) -> Unit): () -> Unit
}
