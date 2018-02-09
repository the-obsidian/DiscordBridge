# DiscordBridge [![Build Status](https://travis-ci.org/the-obsidian/DiscordBridge.svg?branch=master)](https://travis-ci.org/the-obsidian/DiscordBridge)

Bridges chat between Discord and Minecraft.

## Requirements

* Java 8
* Any of:
  * Spigot 1.12
  * Sponge 1.12
  * Forge 1.12

## Installation

1. Download the latest release from the [releases page](https://github.com/the-obsidian/DiscordBridge/releases)
2. Add it to your plugin/mod folder
3. Run your server once to generate configs
4. Find `config.yml` and add required information (see [the guide on the wiki](https://github.com/the-obsidian/DiscordBridge/wiki/Configuration) for details)
5. All done!

## Features

* Instant message relay between Minecraft chat and any one Discord channel
* Customizable message templates
* Discord/Minecraft account linking, so names will be translated
* @mention tags usable from Minecraft
* Issue server console commands from Discord to Minecraft
* Clickable URLs and metadata tooltip for attachments sent over Discord
* Discord-flavored Markdown in Minecraft
* [Multiverse-Core](https://www.spigotmc.org/resources/multiverse-core.390/) integration for world aliases
* [Dynmap](https://www.spigotmc.org/resources/dynmap.274/) chat integration (Bukkit-only for now)
* Misc bot commands for fun, including optional Cleverbot integration

## Permissions

***NOTE:*** Only the Spigot version supports permission nodes at this time.

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

* Support for Discord embeds
* Make all URLs clickable, not just attachments
* Emoji translation
* Multiple Discord channel support
* Permissions for "@here" and "@everyone"
* Integration with other chat plugins/mods

## Similar Projects

####Bukkit
* [DiscordSRV](https://github.com/Scarsz/DiscordSRV) by Scarsz
* [DiscordBridge](https://github.com/BantaGaming/DiscordBridge) by BantaGaming

####Sponge
* [DiscordBridge](https://github.com/nguyenquyhy/DiscordBridge) by nguyenquyhy

####Forge
* [DiscordChat](https://github.com/shadowfacts/DiscordChat) by Shadowfacts
* [DiscordIntegration](https://github.com/Chikachi/DiscordIntegration) by Chikachi
