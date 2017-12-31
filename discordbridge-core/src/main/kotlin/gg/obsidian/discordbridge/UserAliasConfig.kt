package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.UserAlias

/**
 * An accessor object for the users config file
 */
object UserAliasConfig {
    var aliases: List<UserAlias> = mutableListOf()

    /**
     * Load the stored aliases from file into memory
     */
    fun load(db: DiscordBridge) {
        val list = db.getUsersConfig().getList<HashMap<String, Any>>("aliases")
        aliases = list.castTo({UserAlias(it)}) ?:
                throw IllegalStateException("usernames.yml could not be read - list items are not properly formatted")
    }

    /**
     * Adds a new alias to the list and saves the updated list to file
     */
    fun add(db: DiscordBridge, ua: UserAlias) {
        aliases = aliases.plus(ua)
        db.getUsersConfig().put("aliases", aliases)
        db.getUsersConfig().save()
        db.getUsersConfig().load()
    }

    /**
     * Removes an alias from the list and saves the updated list to file
     */
    fun remove(db: DiscordBridge, ua: UserAlias) {
        aliases = aliases.minus(ua)
        db.getUsersConfig().put("aliases", aliases)
        db.getUsersConfig().save()
        db.getUsersConfig().load()
    }

    private inline fun <reified T : Any> List<HashMap<String, Any>>.castTo(factory: (HashMap<String, Any>) -> T): List<T> {
        return this.mapTo(mutableListOf()) { factory(it) }.toList()
    }
}