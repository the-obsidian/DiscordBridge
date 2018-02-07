package gg.obsidian.discordbridge.discord

import gg.obsidian.discordbridge.DiscordBridge
import gg.obsidian.discordbridge.command.DiscordMessageWrapper
import gg.obsidian.discordbridge.command.controller.BotControllerManager
import gg.obsidian.discordbridge.command.controller.FunCommandsController
import gg.obsidian.discordbridge.command.controller.UtilCommandsController
import gg.obsidian.discordbridge.util.UrlAttachment
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * Listens for events from Discord
 */
class Listener: ListenerAdapter() {
    private val controllerManager = BotControllerManager()

    init {
        controllerManager.registerController(FunCommandsController(), discordExclusive = true, chatExclusive = true)
        controllerManager.registerController(UtilCommandsController(), discordExclusive = true, chatExclusive = true)
    }

    /**
     * Callback for captured messages
     *
     * @param event the MessageReceivedEvent object
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        DiscordBridge.logDebug("Received message ${event.message.id} from Discord - ${event.message.rawContent}")

        // Immediately throw out messages sent from itself
        if (event.author.id == Connection.JDA.selfUser.id) {
            DiscordBridge.logDebug("Ignoring message ${event.message.id} from Discord: it matches this bot's username")
            return
        }

        // Handle attachments
        if (event.message.attachments.isNotEmpty()) {
            DiscordBridge.logDebug("Message has one or more attachments")

            if (event.message.guild == Connection.server
                    && event.message.isFromType(ChannelType.TEXT)
                    && event.message.textChannel == Connection.getRelayChannel()) {

                if (event.message.rawContent.isBlank()) {
                    DiscordBridge.logDebug("Attachment message has no text; sending attachment only")
                    processAttachments(event)
                }
                else {
                    DiscordBridge.logDebug("Attachment message has text; sending attachments after initial text")
                    controllerManager.dispatchMessage(DiscordMessageWrapper(event.message))
                    processAttachments(event)
                }
            }
        }

        else controllerManager.dispatchMessage(DiscordMessageWrapper(event.message))
    }

    /**
     * Takes the attachments on a Message and sends a clickable link to Minecraft for each one
     *
     * @param event the MessageRecievedEvent for the message
     */
    private fun processAttachments(event:MessageReceivedEvent) {
        for (a in event.message.attachments) {
            DiscordBridge.logDebug("Broadcasting attachment url from Discord to Minecraft as user ${event.author.name}")

            val match = Regex("""^.*(\.\w*)$""").matchEntire(a.fileName)
            val fileType = if (match != null)
                when (match.groupValues[1].toLowerCase()) {
                // Images
                    ".jpg", ".jpeg" -> "JPEG image"
                    ".png" -> "PNG image"
                    ".bmp" -> "Bitmap image"
                    ".gif" -> "GIF image"
                    ".webp" -> "WebP image"

                // Audio
                    ".mp3" -> "MPEG-3 audio"
                    ".ogg" -> "OGG Vorbis audio"
                    ".m4a" -> "MPEG-4 audio"
                    ".wav" -> "WAV audio"
                    ".wma" -> "WMA audio"
                    ".flac" -> "FLAC audio"

                // Video
                    ".mov" -> "QuickTime video (MOV)"
                    ".mp4" -> "MPEG-4 video"
                    ".webm" -> "WebM video"
                    ".mkv" -> "Matroska video"
                    ".flv" -> "Flash video"

                // Text/data/programming
                    ".txt" -> "Text file"
                    ".yml", "yaml" -> "YAML file"
                    ".html", ".htm" -> "HTML file"
                    ".js" -> "JavaScript file"
                    ".java" -> "Java file"
                    ".cs" -> "C# file"
                    ".py" -> "Python file"
                    ".bat" -> "Batch file"

                // Archives
                    ".zip" -> "ZIP archive"
                    ".rar" -> "RAR archive"
                    ".7z" -> "7zip archive"
                    ".tar" -> "Tarball"
                    ".gz" -> "Gzip archive"
                    ".jar" -> "Java archive"

                // Other common types
                    ".exe" -> "Executable file"
                    ".pdf" -> "Adobe PDF file"
                    ".doc" -> "Microsoft Word DOC file"
                    ".docx" -> "Microsoft Word DOCX file"
                    ".ppt" -> "Microsoft PowerPoint PPT file"
                    ".pptx" -> "Microsoft PowerPoint PPTX file"
                    ".xls" -> "Microsfot Excel XLS file"
                    ".xlsx" -> "Microsoft Excel XLSX file"

                // Minecraft types
                    ".schematic" -> "MCEdit Schematic file"
                    ".properties" -> "Minecraft server properties file"
                    ".mcmeta" -> "Minecraft metadata file"
                    ".dat" -> "NBT data file"

                    else -> match.groupValues[1].toUpperCase().substring(1) + " file"
                } else "unknown type"

            val fileName = if (a.fileName.length > 34) a.fileName.substring(0..30)+"..." else a.fileName

            val fileSize = when (a.size) {
                in 0..1023 -> "${a.size} bytes"
                in 1024..1048575 -> "${String.format("%.2f", a.size / 1024.0)} KiB"
                in 1048576..1073741823 -> "${String.format("%.2f", a.size / 1048576.0)} MiB"
                else -> "${String.format("%.2", a.size / 1073741824.0)} GiB"
            }

            var hoverText = "Name: $fileName\nType: $fileType\nSize: $fileSize"
            if (a.isImage) hoverText += "\nDimensions: ${a.height}x${a.width}"

            val senderName = DiscordBridge.translateAliasesToMinecraft(event.author.name)
            val att = UrlAttachment(senderName, a.url, hoverText)
            DiscordBridge.sendToMinecraft(att)
        }
    }
}
