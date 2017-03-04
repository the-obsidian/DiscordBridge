package gg.obsidian.discordbridge.minecraft

import org.bukkit.entity.Player

enum class Permissions(val node: String) {
    reload("discordbridge.reload"),
    cleverbot("discordbridge.cleverbot"),
    f("discordbridge.f"),
    eightball("discordbridge.eightball"),
    insult("discordbridge.insult"),
    rate("discordbridge.rate");

    fun has(player: Player): Boolean {
        return player.hasPermission(node)
    }
}
