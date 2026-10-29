# WhitelistDb Mod

## Description
This mod is used to make it easier to have the same whitelist across different servers and to use different methods for allowing users to whitelist.
For example, having people in a school Minecraft server, and in order for them to join, they have to verify their school email to gain access to a whitelist command from a Discord bot. (Please note that this does not provide a Discord bot.)

## Commands
* /whitelistdb toggle
  * Turns the whitelist on and off
* /wban <player> [reason]
  * Updates the database record to show they are banned across **all servers!**
  * Will kick the player if they are online - does work on players who aren't online
  * The reason is optional and is only saved when `ban_reasons_enabled` is `true` (see below)
  * Names with spaces can be put in quotes: `/wban "Some Name" griefing`
* /wunban <player>
  * Updates the database record to show they are unbanned across **all servers!**
  * Also clears the saved ban reason when ban reasons are enabled

## Getting Started
To use this mod correctly, the table needs to at least have this information in it.
```
uuid : UUID
banned : Boolean (can be called something else - set ban_column in the config)
tag : String (Optional - for placeholder api, can be called something other than tag)
ban_reason : String (Optional - only needed if ban_reasons_enabled is true, name set with ban_reason_column)
```

This mod also caches usernames and UUIDs locally to ban/unban offline players much more easily.
If this is being added to a server that is already up, you can use the Python script provided in `scripts/convert.py` to correctly take the `usercache.json` file in the root directory of the server and make a file that should be placed in `config/whitelistdb`. This isn't completely necessary, as the cache will auto-update when new people join, but this will make sure that everyone who has ever joined will be detected by the mod.

When the mod is in the mod folder, start the server, and it will make a file located at `config/whitelistdb-config.json`. This is where you can edit the default values to correctly set up the database and edit the messages that come up when not whitelisted and when users are banned.

Default layout of `config/whitelistdb-config.json`
```
{
  "host": "localhost",
  "port": 5432,
  "database": "minecraft",
  "username": "postgres",
  "password": "password",
  "table": "server_whitelists",
  "placeholder_column": "tag",
  "ban_column": "banned",
  "ban_reasons_enabled": false,
  "ban_reason_column": "ban_reason",
  "ssl": false,
  "message": "You are not whitelisted!",
  "enabled": true,
  "banReason": "You have been banned!",
  "banMessageWithReason": "You have been banned!\nReason: %reason%"
}
```
If you're updating from an older version, the new settings are added to your existing config file automatically the next time the server starts.

### Ban reasons
* `ban_column` - the name of the true/false column that marks a player as banned.
* `ban_reasons_enabled` - set to `true` to save a reason with each ban and show it when the player is kicked. Off by default.
* `ban_reason_column` - the text column the reason is saved in. Add it to your table before turning reasons on, e.g.
  `ALTER TABLE server_whitelists ADD COLUMN ban_reason text;`
* `banMessageWithReason` - the kick message used when a ban has a reason. `%reason%` is replaced with the reason and `\n` starts a new line.
  Bans without a reason (or with reasons turned off) use `banReason` instead.

The placeholder for this mod is `%whitelistdb:playerinfo%`.
* On 1.21.x the old `%whitelistdb:school%` placeholder also still works.

Please note that currently neoforge **does not** support placeholders!

## Building
Each platform is split into two modules:
* `fabric` / `neoforge` / `paper` - 26.x, Java 25
* `fabric-1.21` / `neoforge-1.21` / `paper-1.21` - 1.21 - 1.21.11, Java 21

Every module uses the same `-PminecraftVersion` property and only reacts to versions it supports
(1.x versions go to the `-1.21` modules, 26.x versions to the others).

```
# One version
./gradlew :fabric:build -PminecraftVersion=26.3
./gradlew :neoforge:build -PminecraftVersion=26.3
./gradlew :paper:build -PminecraftVersion=26.3
./gradlew :fabric-1.21:build -PminecraftVersion=1.21.5
./gradlew :neoforge-1.21:build -PminecraftVersion=1.21.5
./gradlew :paper-1.21:build -PminecraftVersion=1.21.5

# Every version
./gradlew :fabric:buildAllVersions
./gradlew :neoforge:buildAllVersions
./gradlew :paper:buildAllVersions
./gradlew :fabric-1.21:buildAllVersions
./gradlew :neoforge-1.21:buildAllVersions
./gradlew :paper-1.21:buildAllVersions
```
Jars end up in each module's `build/libs` folder.

## Mod Information
Current version: 1.8.0

Currently Supported:
* Fabric: 1.21 - 1.21.11, 26.1 - 26.3
* NeoForge: 1.21 - 1.21.11, 26.1 - 26.3
* Paper: 1.21 - 1.21.11, 26.1.1 - 26.3 (Paper never released 1.21.2 or 26.1)

