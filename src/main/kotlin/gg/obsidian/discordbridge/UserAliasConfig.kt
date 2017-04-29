package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.utils.UserAlias

/**
 * An accessor object for the users config file
 */
object UserAliasConfig {
    var aliases: List<UserAlias> = mutableListOf()

    /**
     * Load the stored aliases from file into memory
     */
    fun load(plugin: Plugin) {
        val list = plugin.users.data.getList("aliases")
        if (list != null) aliases = list.checkItemsAre<UserAlias>() ?:
                throw IllegalStateException("usernames.yml could not be read - list items are not properly formatted")
        else mutableListOf<UserAlias>()
    }

    /**
     * Adds a new alias to the list and saves the updated list to file
     */
    fun add(plugin: Plugin, ua: UserAlias) {
        aliases = aliases.plus(ua)
        plugin.users.data.set("aliases", aliases)
        plugin.users.saveConfig()
        plugin.users.reloadConfig()
    }

    /**
     * Removes an alias from the list and saves the updated list to file
     */
    fun remove(plugin: Plugin, ua: UserAlias) {
        aliases = aliases.minus(ua)
        plugin.users.data.set("aliases", aliases)
        plugin.users.saveConfig()
        plugin.users.reloadConfig()
    }

    /**
     * A function to assert that all the items in a given list are of a specific type
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null
}
