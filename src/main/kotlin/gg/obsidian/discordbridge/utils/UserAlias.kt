package gg.obsidian.discordbridge.utils

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import java.util.*

//TODO: make null safe
/**
 * Represents a UserAlias entry in usernames.yml
 *
 * @param mcUuid the UUID of the Minecraft account linked to this alias
 * @param discordId the unique Discord ID of the Discord account linked to this alias
 */
@SerializableAs("UserAlias")
data class UserAlias(val mcUuid: UUID, val discordId: String): ConfigurationSerializable {
    @Suppress("unused")
    constructor(map: MutableMap<String, Any>):
            this(UUID.fromString(map["mcUuid"] as String), map["discordId"] as String)

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf("mcUuid" to mcUuid.toString(), "discordId" to discordId)
    }

}