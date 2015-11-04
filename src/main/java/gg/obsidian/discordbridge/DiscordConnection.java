package gg.obsidian.discordbridge;

import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.DiscordBuilder;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.talkable.Group;

public class DiscordConnection implements Runnable {

    public final DiscordBridge plugin;
    public DiscordAPI api;
    public Server server;
    public Group channel;

    public DiscordConnection(DiscordBridge plugin) {
        this.plugin = plugin;
    }

    public void run() {
        try {
            api = new DiscordBuilder(plugin.email, plugin.password).build().login();
            api.getEventManager().registerListener(new DiscordListener(plugin, api));
        } catch (Exception e) {
            plugin.getLogger().severe("Error connecting to Discord: " + e);
        }
    }

    public void send(String name, String message) {
        if (server == null && (server = getServerById(plugin.serverID)) == null) {
            return;
        }

        if (channel == null && (channel = getGroupByName(server, plugin.channel)) == null) {
            return;
        }

        channel.sendMessage("<" + name + "> " + message);
    }

    private Server getServerById(String id) {
        for (Server server : api.getAvailableServers())
            if (server.getId().equals(id))
                return server;
        return null;
    }

    private Group getGroupByName(Server server, String name) {
        for (Group group : server.getGroups())
            if (group.getName().equals(name))
                return group;
        return null;
    }
}
