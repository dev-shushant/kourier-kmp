package dev.shushant.kourier.ui.export

import androidx.core.content.FileProvider

/**
 * Dedicated FileProvider subclass for Kourier.
 *
 * Using an explicit subclass prevents manifest merger collisions when the host application
 * or another library also declares androidx.core.content.FileProvider.
 */
class KourierFileProvider : FileProvider()
