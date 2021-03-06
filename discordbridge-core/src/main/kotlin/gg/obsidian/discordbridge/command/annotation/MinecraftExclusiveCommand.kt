package gg.obsidian.discordbridge.command.annotation

/**
 * Annotates a BotCommand as a command that is only exposed as a Minecraft console command
 *
 * @see BotCommand
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinecraftExclusiveCommand
