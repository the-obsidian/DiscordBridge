package co.orre.discordbridge.commands.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotCommand(val usage: String, val description: String, val name: String = "",
                            val relayTriggerMessage: Boolean = true, val ignoreExcessArguments: Boolean = true)
