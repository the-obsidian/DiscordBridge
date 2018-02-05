package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.Cfg
import gg.obsidian.discordbridge.util.UserAlias

/**
 * An accessor object for the users config file
 */
object UserAliasConfig {
    var aliases: List<UserAlias> = mutableListOf()

    /**
     * Load the stored aliases from file into memory
     */
    fun load() {
        val list = DiscordBridge.getConfig(Cfg.ALIAS).getList<Map<String, Any>>("aliases")
        aliases = list.castTo({UserAlias(it)})
    }

    /**
     * Adds a new alias to the list and saves the updated list to file
     */
    fun add(ua: UserAlias) {
        aliases = aliases.plus(ua)
        DiscordBridge.getConfig(Cfg.ALIAS).put("aliases", aliases)
        DiscordBridge.getConfig(Cfg.ALIAS).save()
        DiscordBridge.getConfig(Cfg.ALIAS).load()
    }

    /**
     * Removes an alias from the list and saves the updated list to file
     */
    fun remove(ua: UserAlias) {
        aliases = aliases.minus(ua)
        DiscordBridge.getConfig(Cfg.ALIAS).put("aliases", aliases)
        DiscordBridge.getConfig(Cfg.ALIAS).save()
        DiscordBridge.getConfig(Cfg.ALIAS).load()
    }

    private inline fun <reified T : Any> List<Map<String, Any>>.castTo(factory: (Map<String, Any>) -> T): List<T> {
        return this.mapTo(mutableListOf()) { factory(it) }.toList()
    }
}