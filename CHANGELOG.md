# Changes in 2.0 - Alpha 2

### This is an alpha release!

### Before updating the mod, always backup your whitelist.json!

## Fixes

- `/link` and `/linkinfo` no longer register as requiring admin permission
  - For some reason, passing 0 as the permission bitset defaults to admin only...
- Locks that never expire no longer show a date in the past
  - instead it will say it expires in ~31k years. This is a temporary fix, it will be changed to say "never" in the future

## Changes
- Added the `/userlinkinfo` command
  - It allows an admin to view a specific user's link
  - The command requires the admin to have the kick member permission be default 
  - It has 2 sub commands, `user` and `username`
  - `user` allows viewing the link of a Discord user
  - `username` allows viewing the link of the given in game username
  - `username` shows an autocomplete with currently whitelisted usernames
  - This command is planned into being merged into `/userlink` as a sub-command group.
- Updated the `autowhitelist rebuild-from-cache` in game command
  - The command now adds all qualifying cached entries, instead of just removing whitelist entries and letting the cache on login do the rest
  - The option to not run actions has been removed. The new implementation doesn't support not running the `add` actions.
- Added the `/status` command
  - This command shows the status of several parts of the mod environment.
  - There are 6 sub-commands
  - `minecraft` shows details about the game and loader.
  - `networking` shows details about the Minecraft server networking, this will be updated once multilink is implemented.
  - `whitelist` shows details about the server and mod whitelist.
  - `cache` shows details about the whitelist cache.
  - `bot` shows details about the Discord bot.
  - `config` shows details about the mod's config, this does **not** show the config itself.
  - The command currently has a hardcoded text and layout, this will be changed once the new system for the Discord messages is implemented.

Please provide feedback for the current command structure and naming.
