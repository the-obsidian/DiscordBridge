package gg.obsidian.discordbridge;

import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.event.EventListener;
import me.itsghost.jdiscord.events.UserChatEvent;
import org.bukkit.ChatColor;

public class DiscordListener implements EventListener {

    public final DiscordBridge plugin;
    public final DiscordAPI api;

    public DiscordListener(DiscordBridge instance, DiscordAPI api) {
        this.plugin = instance;
        this.api = api;
    }

    public void userChat(UserChatEvent e) {
        if (!e.getServer().getId().equals(plugin.serverID)) {
            return;
        }

        if (!e.getGroup().getName().equalsIgnoreCase(plugin.channel)) {
            return;
        }

        String username = e.getUser().getUser().getUsername();

        if (username.equalsIgnoreCase(plugin.username)) {
            return;
        }

        String broadcastMessage = "<" +
                username +
                ChatColor.AQUA +
                "(discord)" +
                ChatColor.RESET +
                "> " +
                e.getMsg().getMessage();

        plugin.getServer().broadcastMessage(broadcastMessage);
    }
}
