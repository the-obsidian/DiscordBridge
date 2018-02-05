package gg.obsidian.discordbridge.util

//TODO: make null safe
/**
 * Represents a UserAlias entry in usernames.yml
 *
 * @param mcUuid the UUID of the Minecraft account linked to this alias
 * @param discordId the unique Discord ID of the Discord account linked to this alias
 */
data class UserAlias(var mcUuid: String, var discordId: String) {
    constructor(map: Map<String, Any>): this(map["mcUuid"] as String, map["discordId"] as String)
}