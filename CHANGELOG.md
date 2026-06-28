# Changes in 2.0 - Alpha 1

### This is an alpha release!

### Before updating the mod, always backup your whitelist.json!

## Alpha notes

- The alpha will only be available for 26.1 and later 26.2, if both will be supported at once is uncertain.
- Many features aren't available yet, they will be added over time during the alpha stage.
- The Discord responses are currently handled trough translations and have a fixed layout. This format may change in the future.
- Admin discord commands are not yet available, they will be added in a future alpha version.
- Several translation keys were changed. Backup your translation datapack and generate a new one for the new messages.
- The Minecraft Server Management Protocol support is not fully developed and may have issues, use at your own risk.
- The mod is currently using a tool that converts the translation file from `yml` to `json` on build time. The generated translations JSON may differ from the
  original file.
- The GeyserMC support uses the Geyser Global Cache to get a user's xuid from the username, currently it will fail if the username is not cached. \
  There are plans for improving on this in a future alpha version.
- The GeyserMC support has not been fully re-tested yet.

## General

- It is now possible to disable the discord bot by setting the token to `null`
- Fabric Language Kotlin is no longer required

## Config

The config is not final and will change during the alphas. \
Only the last version is kept, **keep a backup of your 1.x config!**
In the case that the config is split into multiple files, the v1 config is considered legacy and the `autowhitelist` folder will be reset

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
  - For a lower impact on connection time the cache now uses the internal cache exclusively. \
    This is currently being tested, if you encounter a case where a Discord user was not in the cache when the should please report it.
- The not whitelisted disconnect message now has a line appended to the end
  - The message is controlled by the translation `multiplayer.autowhitelist.disconnect.not_whitelisted.tip`, the `.geyser` key suffix is used if GeyserMC and Floodgate are present
  - The message will always use the server language, this is a limitation due to it being used early in the  
- Whitelisted usernames are updated on connection
  - When a player logs in, if the whitelist entry for their UUID doesn't match their username, it will then be updated
    and saved into the whitelist file
- Removed the `/autowhitelist fix-duplicate-commands` command
- Added `/awutowhitelist rebuild-from-cache` which goes through the whitelist cache to update the whitelist with linked entries
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
  - When getting a profile by its name, when it is not found in the Geyser cache, then `mcprofile.io` will be used as a fallback
