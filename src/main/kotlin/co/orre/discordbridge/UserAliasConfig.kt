package co.orre.discordbridge

import co.orre.discordbridge.utils.UserAlias

object UserAliasConfig {
    var aliases: List<UserAlias> = mutableListOf()

    fun load(plugin: Plugin) {
        val list = plugin.users.data.getList("aliases")
        if (list != null) aliases = list.checkItemsAre<UserAlias>() ?:
                throw IllegalStateException("usernames.yml could not be read - list items are not properly formatted")
        else mutableListOf<UserAlias>()
    }

    fun add(plugin: Plugin, ua: UserAlias) {
        aliases = aliases.plus(ua)
        plugin.users.data.set("aliases", aliases)
        plugin.users.saveConfig()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> List<*>.checkItemsAre() = if (all { it is T }) this as List<T> else null
}
