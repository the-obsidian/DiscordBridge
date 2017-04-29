package gg.obsidian.discordbridge.commands.annotations

/**
 * Annotates a function as a command the bot can run.
 *
 * @param usage a short string that describes the command's parameter syntax
 * @param description a short string that describes the command's function
 * @param name an optional field to override the command's access name if it is not the same as the method name
 * @param relayTriggerMessage whether the message used to trigger this command should be relayed
 * @param ignoreExcessArguments if false, this command will fail if the invoker provides too many arguments
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotCommand(val usage: String, val description: String, val name: String = "",
                            val relayTriggerMessage: Boolean = true, val ignoreExcessArguments: Boolean = true)
