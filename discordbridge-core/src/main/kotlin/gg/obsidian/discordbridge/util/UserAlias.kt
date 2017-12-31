package gg.obsidian.discordbridge.util

import java.io.Serializable
import java.util.*

//TODO: make null safe
/**
 * Represents a UserAlias entry in usernames.yml
 *
 * @param mcUuid the UUID of the Minecraft account linked to this alias
 * @param discordId the unique Discord ID of the Discord account linked to this alias
 */
data class UserAlias(val mcUuid: UUID, val discordId: String): Serializable {

    constructor(): this(UUID.fromString("null"), "null")

    constructor(map: MutableMap<String, Any>):
            this(UUID.fromString(map["mcUuid"] as String), map["discordId"] as String)

}