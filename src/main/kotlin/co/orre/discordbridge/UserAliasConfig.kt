package co.orre.discordbridge

import co.orre.discordbridge.utils.UserAlias

object UserAliasConfig {
    var aliases: List<UserAlias> = mutableListOf()

    fun load(plugin: Plugin) {
        aliases = plugin.users.data.getList("aliases").checkItemsAre<UserAlias>()
            ?: throw IllegalStateException("usernames.yml could not be read - list items are not properly formatted")
    }

    fun add(plugin: Plugin, ua: UserAlias) {
        aliases = aliases.plus(ua)
        plugin.users.data.set("aliases", aliases)
        plugin.users.saveConfig()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null
}
