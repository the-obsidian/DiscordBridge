package gg.obsidian.discordbridge

import org.bukkit.entity.Player

enum class Permissions(val node: String) {
    reload("discordbridge.reload"),
    cleverbot("discordbridge.cleverbot");

    fun has(player: Player): Boolean {
        return player.hasPermission(node)
    }
}
