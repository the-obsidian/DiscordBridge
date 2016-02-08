# DiscordBridge [![Build Status](https://travis-ci.org/the-obsidian/DiscordBridge.svg?branch=master)](https://travis-ci.org/the-obsidian/DiscordBridge)

Bridges chat between Discord and Minecraft (Bukkit/Spigot).

## Requirements

* Java 8

## Installation

1. Download the [latest release](https://github.com/the-obsidian/DiscordBridge/releases) from GitHub
1. Add it to your `plugins` folder
1. Either run Bukkit/Spigot once to generate `DiscordBridge/config.yml` or create it using the guide below.
1. All done!

## Configuration

DiscordBridge has several options that can be configured in the `config.yml` file:

```yaml
settings:
  server-id: '00000000'
  channel: 'test'
  username: 'username'
  email: 'email@example.com'
  password: 'password'
  debug: false
  relay_cancelled_messages: true
  messages:
    join: true
    leave: true
    death: false
  templates:
    discord:
      chat_message: '<%u> %m'
      player_join: '%u joined the server'
      player_leave: '%u left the server'
      player_death: '%r'
    minecraft:
      chat_message: '<%u&b(discord)&r> %m'
```

* `server-id` is the ID of your Discord server.  This can be found under *Server Settings > Widget > Server ID*
* `channel` is the Discord channel name you would like to bridge with your Minecraft server
* `username` is the Discord username of your bot user
* `email` is the Discord email address of your bot user
* `password` is the Discord password of your bot user
* `debug` enables more verbose logging
* `relay_cancelled_messages` will relay chat messages even if they are cancelled
* `messages` enables or disables certain kinds of messages
* `templates` - customize the message text 

**Templates**

- `%u` will be replaced with the username 
- '%d' will be replaced with the user's display name
- `%m` will be replaced with the message
- `%w` will be replaced with the world name
- `%r` will be replaced with the death reason
- Color codes, prefixed with `&`, will be translated on the Minecraft end

## Features

* Anything said in Minecraft chat will be sent to your chosen Discord channel
* Anything said in your chosen Discord channel will be sent to your Minecraft chat (with a `(discord)` suffix added to usernames)
* Join / leave messages are sent to Discord
* Death messages can optionally be sent to Discord
* Message templates are customized

## Permissions

- `discordbridge.reload` - ability to reload config and reconnect the Discord connection

## Commands

- `/discord reload` - reloads config and reconnects to Discord

## Upcoming Features

* Deeper integration into Minecraft chat (like supporting chat channels inside Minecraft)
* A "merge accounts" function to allow Minecraft players to associate their Discord accounts with their Minecraft accounts so that usernames are properly translated
* Ability to post messages to Discord on behalf of Discord users, rather than using a bot user (hopefully after the official API is released)
