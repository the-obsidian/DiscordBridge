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
  templates:
    discord:
      chat_message: '<%u> %m'
      player_join: '%u joined the server'
      player_leave: '%u left the server'
    minecraft:
      chat_message: '<%u&b(discord)&r> %m'
```

* `server-id` is the ID of your Discord server.  This can be found under *Server Settings > Widget > Server ID*
* `channel` is the Discord channel name you would like to bridge with your Minecraft server
* `username` is the Discord username of your bot user
* `email` is the Discord email address of your bot user
* `password` is the Discord password of your bot user
* `debug` enables more verbose logging
* `templates` - customize the message text - `%u` will be replaced with the username and `%m` will be replaced with the message.  Color codes, prefixed with `&`, will be translated on the Minecraft end.

## Features

* Anything said in Minecraft chat will be sent to your chosen Discord channel
* Anything said in your chosen Discord channel will be sent to your Minecraft chat (with a `(discord)` suffix added to usernames)
* Join / leave messages are sent to Discord
* Message templates are customized

## Upcoming Features

* Deeper integration into Minecraft chat (like supporting chat channels inside Minecraft)
* A "merge accounts" function to allow Minecraft players to associate their Discord accounts with their Minecraft accounts so that usernames are properly translated
* Ability to post messages to Discord on behalf of Discord users, rather than using a bot user (hopefully after the official API is released)
