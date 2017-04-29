package gg.obsidian.discordbridge.commands

/**
 * Controls a number of commands that can be bulk applied to a BotControllerManager
 */
interface IBotController {
    /**
     * @return a short description of this IBotController's methods as seen in the Help command
     */
    fun getDescription(): String
}
