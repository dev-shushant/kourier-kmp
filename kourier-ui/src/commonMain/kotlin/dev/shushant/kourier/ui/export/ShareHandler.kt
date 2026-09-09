package dev.shushant.kourier.ui.export

expect object ShareHandler {
    /** Shares plain text string directly into apps (WhatsApp, Slack, Messages, Notes). */
    fun shareText(title: String, content: String, chooserTitle: String = "Share Text")

    /** Shares content as an attached file (.txt, .har, .json) via FileProvider on Android or temporary URL on iOS. */
    fun shareFile(fileName: String, content: String, mimeType: String = "text/plain", chooserTitle: String = "Share File")
}
