# Changes in 2.0

### Before updating the mod, always backup your whitelist.json!

## General

- Dropped support for 1.20 and 1.20.1
- Fabric Language Kotlin is no longer required
- It is now possible to disable the discord bot by setting the token to `null`
- Added support for the Minecraft Server Management Protocol
  - **This feature is still being worked on and is unpolished**
  - The vanilla method endpoints support the extended AutoWhitelist format
  - The vanilla notification endpoints provide the extended AutoWhitelist format
  - Added an experimental `autowhitelist:allowlist/register` method endpoint to allow for future multiserver support
    with an additional separate mod
  - No notification for role changes has been added yet
  - You can use the `minecraft:allowlist/added` notification event as a "registered notification event"

## Config

Only the last version is kept, **keep a backup of your 1.x config!**

- Reworked the config, separating it into categories
- Updated the config handling to use an improved structure
- Config errors are now cleaner and clearer
- Updated the config to version 7
- The config updater no longer supports configs of version 5 or prior
- Renamed some options for easier understanding
- Improved the comments on some options
- `$schema`, `enable_whitelist_cache`, `periodic_check_delay` and `cache_discord_data` have been removed
- Dropped updater support for versions 1-5
- Added updater for version 6 to version 7
- Whenever the config is updated, the old version is now saved as `autowhitelist.json5.old``
- `entries` is now named `allow`, it has also been updated to support multiple or no actions
  - An entry now takes an `actions` field, which can be an action object or a list of action objects
- Some options now have a hybrid structure, supporting multiple types.
- Certain option structures are version dependent

### The `allow` field

The `roles` field remains within the entry object \
An `action` object holds the rest of the structure of the old entry object, with a `type` field for the action id,
and an `execute` field for action specific options

Check the actions section for the changes on actions

## Minecraft (in-game)

- The whitelist cache now is always enabled
  - If a user had an account previously linked, but is no longer in the whitelist, they will be automatically added to
    the whitelist (if qualified) when they attempt to join the server.
- The not whitelisted disconnect message now has a line appended to the end
  - The message is controlled by the translation `multiplayer.autowhitelist.disconnect.not_whitelisted.tip`, the `.geyser` key suffix is used if GeyserMC and Floodgate are present
  - The message will always use the server language, this is a limitation due to it being used early in the
- Whitelist usernames are updated on connection
  - When a player logs in, if the whitelist entry for their UUID doesn't match their username, it will then be updated
    and saved into the whitelist file
- Removed the `/autowhitelist fix-duplicate-commands` command
- Updated `/autowhitelist create-translations-datapack` to use the `datapack create` command on supported versions
- Updated `/autowhitelist create-translations-datapack` to take the same arguments as `datapack create`
- Added `/awutowhitelist rebuild-from-cache` which goes through the whitelist cache to update the whitelist with linked
  entries
- Added `/autowhitelit remove-all-guild-commands`, this command deletes all the Discord application's guild commands. **This
  command is only present to fix unused or broken application commands, use with caution!**

## Discord integration

- Migrated from JDA to Discord4J
- Rewrote the registration process
  - The process is now cleaner, smoother and a lot easier to expand and implement
- Reworked the interaction handler
- Updated all commands
  - Renamed `register` to `link`
  - Added autocomplete options for the `link` command, these options are suggestions based on the discord username and
    display name
  - Renamed `info` to `my-link`
  - Added `link-info`, an admin command to view a user's link details
    - This command has a chat and user variant
      - User commands show under `Apps` when right-clicking a user
    - The chat command has 2 subcommands, `user` and `username`
      - `user` lets you get a user's link info from a discord profile
      - `username` lets you get a user's link info from a Minecraft username
  - Added `user-link`, an admin command to modify the link of a user
    - The subcommand `modify` allows a moderator to add or modify a user's linked account
    - The subcommand `remove` allows a moderator to remove a user's linked account
- Improved replies
  - Updated to Components V2
  - Reworked all replies

## GeyserMC integration

- Improved bedrock username handling on registration
  - When getting a profile by its name, when it is not found in the Geyser cache it is then fetched from mcprofile.io
