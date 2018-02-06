package gg.obsidian.discordbridge.command.annotation

/**
 * Annotates a BotCommand as a command that is only exposed to Discord chat
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DiscordExclusiveCommand
