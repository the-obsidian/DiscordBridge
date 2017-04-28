package gg.obsidian.discordbridge.commands.annotations

/**
 * Annotates a BotCommand as a command that is exposed to Discord chat and Minecraft chat
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ChatExclusiveCommand
