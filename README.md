# DiscordBridge [![Build Status](https://travis-ci.org/the-obsidian/DiscordBridge.svg?branch=master)](https://travis-ci.org/the-obsidian/DiscordBridge)

Bridges chat between Discord and Minecraft (Bukkit/Spigot).

## Requirements

* Java 8
* Spigot 1.12

## Installation


1. Download the latest release from GitHub
2. Add it to your plugins folder
3. Either run Bukkit/Spigot once to generate DiscordBridge/config.yml or create it using the guide below.
4. All done!


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

# (Optional) Define an alternate prefix for all of the bot's commands. These will work in addition to @mentions.
# Will also work in Minecraft if the sender has the required permission for the command they try.
# Leave blank to only allow @mentions to prefix commands
command-prefix: ''

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

# Set the templates for various message types
# %u - The username of the one who sent the message or invoked a command, if applicable
# %m - The raw message that would normally display, if applicable
# %w - The name of the world the sender is in
#   - Applicable to messages from Minecraft only
#   - Multiverse alias compatible
# Use '&' in place of the formatting symbol to apply formatting codes to messages sent to Minecraft
templates:
  discord:
    chat-message: '<**%u**> %m'
    player-join: '**%u** joined the server'
    player-leave: '**%u** left the server'
    player-death: '%m'
    server-start: 'Server started!'
    server-stop: 'Shutting down...'
  minecraft:
    chat-message: '[&b&l%w&r]<%u> %m'
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

- `%u` will be replaced with the player/user's username
- `%m` will be replaced with the message
- `%w` will be replaced with the world name
- Color codes, prefixed with `&`, will be translated on the Minecraft end

## Features

* Anything said in Minecraft chat will be sent to your chosen Discord channel
* If Multiverse-Core is installed and the `%w` tag is specified in your relay message syntax, the alias assigned to your Multiverse worlds will be displayed
* Anything said in your chosen Discord channel will be sent to your Minecraft chat (if the `%w` tag is used in your relay message syntax, Discord messages will display `Discord`)
* If Dynmap is installed, anything said over Dynmap chat will be relayed to your chosen Discord channel (if the `%w` tag is used in your relay messag syntax, Dynmap messages will display `Dynmap`)
* You can link Minecraft accounts to Discord accounts and the bot will translate display names to match where the message appears
* Join / leave messages can be sent to Discord
* Death messages can be sent to Discord
* Server start and stop messages can be sent to Discord
* All of the above messages can be toggled if you don't want them to appear
* Message templates are customizeable
* Prefixing usernames with `@` in the Minecraft chat will be converted to tag mentions in the Discord chat if the user exists (you can use their Discord display name with spaces removed, or their Minecraft username if the accounts are linked)
* Add customizeable scripted responses for the bot to say when it detects a trigger phrase
* A handful of fun and shitposting commands for the full Discord Bot experience both in and out of game
* Cleverbot integration - chat with the bot using `@<bot name>` or `/talk`. Works in Discord AND Minecraft! (requires Cleverbot API key)
* The bot can use any of its commands in any channel it can read (including DMs!) allowing it to function as a general-purpose Discord bot on the side
* Command permissions affect both Minecraft slash command and Minecraft in-chat commands

## Permissions

- `discordbridge.discord` - ability to use any command in of the /discord subcommand tree
- `discordbridge.discord.reload` - ability to reload configs and JDA
- `discordbridge.discord.listmembers` - abiliyt to receive a list of members in the Discord channel
- `discordbridge.discord.linkalias` - abiliy to send a request to a Discord member to set up alias translation
- `discordbridge.talk` - ability to talk to Cleverbot
- `discordbridge.f` - ability to use the f command
- `discordbridge.rate` - ability to use the rate command
- `discordbridge.eightball` - ability to use the 8ball command
- `discordbridge.insult` - ability to use the insult command
- `discordbridge.choose` - ability to use the choose command
- `discordbridge.roll` - ability to use the roll command

## Commands

- `8ball <question>` - consult the Magic 8-Ball to answer your yes/no questions (messages configurable in `botmemory.yml`)
- `discord reload` - refreshes the JDA connection and reloads configs
- `discord linkalias` - sends a request to a specified Discord user to link aliases for username translation
- `discord listmembers all` - lists all the members in the Discord relay channel
- `discord listmembers online` - lists all the members in the Discord relay channel who are online along with their statuses
- `discord unlinkalias ` - silently breaks an alias link with a Discord user, if one exists
- `f` - press F to pay respects (messages configurable in `f.yml`)
- `rate <thing to be rated>` - have the bot rate something for you (rating scale and messages configurable in `rate.yml`)
- `insult <thing to insult>` - makes the bot insult something (messages configurable in `insults.yml`) (*WARNING: The supplied insults are quite offensive! Remove permissions for this command or replace the insults if you intend to use this bot on cleaner servers!*)
- `choose <choiceA or choiceB || choiceC, choice D...>` - have the bot make a choice for you
- `roll <sides>` - roll a die with a specified number of sides (up to 100)
- `talk <message` - talk to Cleverbot!

## Upcoming Features

* Vanilla Minecraft admin commands (kick, ban, start, stop, give, etc) usable from Discord
* Add support for a URL shortening service so attachment URLs aren't so flipping long
* Add support for relaying embeds
* Make Discord responses for certain commands return in pretty embeds
* More of the 'fun' commands that literally every Discord bot has (with matching Minecraft commands!)
* Multiple Discord channel support?
