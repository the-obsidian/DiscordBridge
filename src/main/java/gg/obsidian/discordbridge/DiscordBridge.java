package gg.obsidian.discordbridge;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class DiscordBridge extends JavaPlugin implements Listener {

    public String serverID;
    public String channel;
    public String username;
    public String email;
    public String password;

    public DiscordConnection connection;

    public void onEnable() {
        updateConfig(getDescription().getVersion());

        this.serverID = getConfig().getString("settings.server-id");
        this.channel = getConfig().getString("settings.channel");
        this.username = getConfig().getString("settings.username");
        this.email = getConfig().getString("settings.email");
        this.password = getConfig().getString("settings.password");

        this.connection = new DiscordConnection(this);

        getServer().getScheduler().runTaskAsynchronously(this, connection);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        send(event.getPlayer().getName(), event.getMessage());
    }

    public void send(String name, String message) {
        connection.send(name, message);
    }

    public void updateConfig(String version) {
        this.saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        getConfig().set("version", version);
        saveConfig();
    }
}