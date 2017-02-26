# DiscordBridge [![Build Status](https://travis-ci.org/the-obsidian/DiscordBridge.svg?branch=master)](https://travis-ci.org/the-obsidian/DiscordBridge)

Bridges chat between Discord and Minecraft (Bukkit/Spigot).

## Requirements

* Java 8
* Spigot 1.11.2

## Installation

<<Coming soon!>>

## Configuration

DiscordBridge has several options that can be configured in the `data.yml` file:

```yaml
settings:
  server-id: '00000000'
  channel: 'test'
  username: 'username'
  username-color: ''
  token: 'token'
  cleverbot-key: ''
  debug: false
  relay_cancelled_messages: true
  announce_server_start_stop: true
  messages:
    chat: true
    join: true
    leave: true
    death: false
  if_vanished:
    chat: false
    join: false
    leave: false
    death: false
  templates:
    discord:
      chat_message: '<%u> %m'
      player_join: '%u joined the server'
      player_leave: '%u left the server'
      player_death: '%r'
      server_start: 'Server started!'
      server_stop: 'Shutting down...'
    minecraft:
      chat_message: '<%u&b(discord)&r> %m'
```

* `server-id` is the ID of your Discord server.  This can be found under *Server Settings > Widget > Server ID*
* `channel` is the Discord channel name you would like to bridge with your Minecraft server
* `username` is the Discord username of your bot user
* `username_color` is for adding formatting codes to the name of your bot when it speaks in Minecraft's chat (optional)
* `token` is the access token for the Discord bot
* `cleverbot-key` is the access key necessary to chat with Cleverbot's API (optional)
* `debug` enables more verbose logging
* `relay_cancelled_messages` will relay chat messages even if they are cancelled
* `messages` enables or disables certain kinds of messages
* `if_vanished` enables or disables messages if the user is vanished (applies after `messages`)
* `templates` - customize the message text 

**Templates**

- `%u` will be replaced with the username 
- `%d` will be replaced with the user's display name
- `%m` will be replaced with the message
- `%w` will be replaced with the world name
- `%r` will be replaced with the death reason
- Color codes, prefixed with `&`, will be translated on the Minecraft end

## Features

* Anything said in Minecraft chat will be sent to your chosen Discord channel
* Anything said in your chosen Discord channel will be sent to your Minecraft chat (with a `(discord)` suffix added to usernames)
* You can link Minecraft accounts to Discord accounts and the bot will translate display names to match where the message appears
* Join / leave messages are sent to Discord
* Death messages are sent to Discord
* Server start and stop messages are sent to Discord
* All of the above messages can be toggled if you don't want them to appear
* Message templates are customized
* Prefixing usernames with `@` in the Minecraft chat will be converted to mentions in the Discord chat if the user exists (you can use their Discord display name with spaces removed, or their Minecraft username if the accounts are linked)
* Image attachments sent in the Discord channel will relay their URLs to Minecraft
* Add scripted responses for the bot to say when it detects a trigger phrase
* Cleverbot integration - chat with the bot on Discord using `@<mention>` or chat with the bot in Minecraft using a slash command

## Permissions

- `discordbridge.reload` - ability to reload data and reconnect the Discord connection

## Commands

- `/discord reload` - reloads data and reconnects to Discord
- `/discord get online` - provides a list of all Discord users in the relay channel who are Online, Do Not Disturb, and Idle
- `/discord get ids` - provides a list of the Discord IDs of all users in the relay channel, which is useful for...
- `/discord register <discord-id>` - this command will send a DM to the corresponding user asking if that user wishes to link their Discord account with the Minecraft user who issued the command
- `/marina <message>` - Talk to Cleverbot! Anything you say after this command is relayed to Cleverbot's API, and the bot will speak the response. Only works if you specify a Cleverbot API key in the config.

## Upcoming Features

* Add support for a URL shortening service so attachment URLs aren't so flipping long
* Some of the 'fun' commands that literally every Discord bot has (with matching Minecraft commands!)
