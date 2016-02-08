package gg.obsidian.discordbridge

import org.bukkit.entity.Player

enum class Permissions(val node: String) {
    reload("discordbridge.reload");

    fun has(player: Player): Boolean {
        return player.hasPermission(node)
    }
}
