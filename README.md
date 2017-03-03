# DiscordBridge [![Build Status](https://travis-ci.org/the-obsidian/DiscordBridge.svg?branch=master)](https://travis-ci.org/the-obsidian/DiscordBridge)

Bridges chat between Discord and Minecraft (Bukkit/Spigot).

## Requirements

* Java 8
* Spigot 1.11.2

## Installation

<<Coming soon!>>

## Configuration

DiscordBridge has several options that can be configured in the `config.yml` file:

```yaml
# DiscordBridge Config

# The bot's Discord API access token
token: ''

# These values will control which channel the bot will watch to relay messages to and from the server.
server-id: '00000000'
channel: 'channel-name'

# The bot's Discord username
username: 'DiscordBridge'

# (Optional) Apply formatting codes to the bot's name in Minecraft chat.
# Use '&' in place of the formatting symbol.
username-color: ''

# (Optional) Set this value with a valid Cleverbot API key to enable chatting with Cleverbot
# Look at https://www.cleverbot.com/api/ for more info
cleverbot-key: ''

# If true, prints verbose log messages to the server console for every action
debug: false

# If true, Minecraft chat events that are cancelled will still be relayed to Discord.
relay-cancelled-messages: true

# Controls which events in general are relayed to Discord
messages:
  player-chat: true
  player-join: true
  player-leave: true
  player-death: false
  server-start: true
  server-stop: true

# Controls which events caused by vanished players are relayed to Discord
# NOTE: If set to true, it will only have effect if the corresponding message above is also set to true
if-vanished:
  player-chat: false
  player-join: false
  player-leave: false
  player-death: false

# Set the templates for relayed messages
# %u - The sender's username
# %m - The sender's message
# %w - The name of the world the sender is in (Multiverse alias compatible)
# %r - The death message (Death event only)
# Use '&' in place of the formatting symbol to apply formatting codes.
templates:
  discord:
    chat-message: '<**%u**> %m'
    player-join: '**%u** joined the server'
    player-leave: '**%u** left the server'
    player-death: '%r'
    server-start: 'Server started!'
    server-stop: 'Shutting down...'
  minecraft:
    chat-message: '[&b&lDiscord&r]<%u> %m'
```

* `token` is the access token for the Discord bot. Without this, the bot will not function at all.
* `server-id` is the ID of the Discord server with the channel you want to bridge.  This can be found under *Server Settings > Widget > Server ID*
* `channel` is the Discord channel name you would like to bridge with your Minecraft server
* `username` is the Discord username of your bot
* `username_color` is for adding formatting codes to the name of your bot when it speaks in Minecraft's chat (optional)
* `cleverbot-key` (optional) an access key necessary to chat with Cleverbot's API
* `debug` enables verbose logging
* `relay_cancelled_messages` will relay chat messages even if they are cancelled
* `messages` enables or disables certain kinds of messages
* `if_vanished` enables or disables messages if the user is vanished (applies after `messages`)
* `templates` - customize the message text 

**Templates**

- `%u` will be replaced with the username 
- `%d` will be replaced with the user's display name
- `%m` will be replaced with the message
- `%w` will be replaced with the world name
- `%r` will be replaced with Minecraft's standard death message
- Color codes, prefixed with `&`, will be translated on the Minecraft end

## Features

* Anything said in Minecraft chat will be sent to your chosen Discord channel
* Anything said in your chosen Discord channel will be sent to your Minecraft chat (with a `(discord)` suffix added to usernames)
* You can link Minecraft accounts to Discord accounts and the bot will translate display names to match where the message appears
* Join / leave messages can be sent to Discord
* Death messages can be sent to Discord
* Server start and stop messages can be sent to Discord
* All of the above messages can be toggled if you don't want them to appear
* Message templates are customized
* Prefixing usernames with `@` in the Minecraft chat will be converted to mentions in the Discord chat if the user exists (you can use their Discord display name with spaces removed, or their Minecraft username if the accounts are linked)
* Image attachments sent in the Discord channel will relay their URLs to Minecraft
* Add scripted responses for the bot to say when it detects a trigger phrase
* A handful of fun and shitposting commands for the full Discord Bot experience both in and out of game
* Cleverbot integration - chat with the bot using `@<name>`. Works in Discord AND Minecraft!
* The bot can use any of its commands in any channel it can read (including DMs!) allowing it to function as a general-purpose Discord bot on the side

## Permissions

- `discordbridge.cleverbot` - ability to talk to Cleverbot
- `discordbridge.f` - ability to pay respects with /f
- `discordbridge.reload` - ability to reload data and reconnect the Discord connection
- `discordbridge.eightball` - ability to consult the Magic 8-Ball with /8ball
- `discordbridge.rate` - ability to ask for an out-of-10 rating with /rate
- `discordbridge.insult` - ability to make the bot insult something with /insult

## Commands

- `/discord reload` - reloads data and reconnects to Discord
- `/discord get online` - provides a list of all Discord users in the relay channel who are Online, Do Not Disturb, and Idle
- `/discord get ids` - provides a list of the Discord IDs of all users in the relay channel, which is useful for...
- `/discord register <discord-id>` - this command will send a DM to the corresponding user asking if that user wishes to link their Discord account with the Minecraft user who issued the command
- `/f` - press F to pay respects
- `/8ball <query>` - consults the Magic 8-Ball to answer your yes/no questions (messages configurable in `botmemory.yml`)
- `/rate <thing to rate>` - asks the bot to rate something on a 0-10 scale
- `/insult <thing to insult>` - makes the bot insult something (messages configurable in `insults.yml`) (*WARNING: The supplied insults are quite offensive! Remove permissions for this command or replace the insults if you intend to use this bot on cleaner servers!*)

## Upcoming Features

* Add support for a URL shortening service so attachment URLs aren't so flipping long
* Some of the 'fun' commands that literally every Discord bot has (with matching Minecraft commands!)
