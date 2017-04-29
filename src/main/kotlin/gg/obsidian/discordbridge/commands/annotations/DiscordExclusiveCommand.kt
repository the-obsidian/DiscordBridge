package gg.obsidian.discordbridge.commands.annotations

/**
 * Annotates a BotCommand as a command that is only exposed to Discord chat
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DiscordExclusiveCommand
