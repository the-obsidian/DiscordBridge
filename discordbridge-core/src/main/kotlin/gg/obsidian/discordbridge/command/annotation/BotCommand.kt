package gg.obsidian.discordbridge.command.annotation

/**
 * Annotates a function as a command the bot can run.
 *
 * @param aliases an optional field to override the command's access name if it is not the same as the method name
 * @param usage a short string that describes the command's parameter syntax
 * @param desc a short string that describes the command's function
 * @param relayTriggerMessage whether the message used to trigger this command should be relayed
 * @param squishExcessArgs if true, this command will put all extra args passed to it into a single string
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotCommand(
        val aliases: Array<String>,
        val desc: String,
        val usage: String = "",
        val help: String = "",
        val relayTriggerMessage: Boolean = true,
        val squishExcessArgs: Boolean = false,
        val ignoreExcessArgs: Boolean = false
)
