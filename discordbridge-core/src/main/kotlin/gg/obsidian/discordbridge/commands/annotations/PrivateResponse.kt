package gg.obsidian.discordbridge.commands.annotations

/**
 * Annotates a BotCommand as a command that will return its output privately to the invoker using
 * whatever medium is appropriate for the invocation source
 *
 * Discord chat commands will return in a DM to the invoker
 *
 * Minecraft chat commands will return as a Minecraft PM to the invoker
 *
 * Minecraft console commands will return as a Minecraft PM to the invoker
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivateResponse