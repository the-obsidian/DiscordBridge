package gg.obsidian.discordbridge.command.annotation

/**
 * Annotates a BotCommand as a command where the response will be prepended with "@invokerName | "
 *
 * In Discord, "@invoker" will be converted to a tag mention if that user exists in the relay channel
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaggedResponse