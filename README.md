![fabric-api](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg)

# AutoWhitelist 2.0 is currently in alpha and the docs may not be updated yet.
The bot setup process is still the same.

## ⚙️ Features
- **Discord link** - allow users to add their Minecraft accounts to the whitelist with access filtered by Discord roles
- **Whitelist actions** - execute actions when whitelisting/removing a player, like commands, permissions or roles
- **Whitelist cache** - keep memory of the usernames, and automatically whitelist the players when they log in if they gained the required role back
- **Detailed documentation** - a complete documentation with images, GIFs and a full tutorial for setting up the Discord bot and the mod
- **Custom messages** - easily customize the mod messages
- **Extendable** - the mod offers an API for mods to add more whitelist actions
- **Built-in compatibility** - the mod comes with built-in compatibility for LuckPerms and Player Roles²
- **Fast and seamless** - the mod works closely to the vanilla whitelist, being compatible with other mods that work with the whitelist, and adds minimal time to logins
- **GeyserMC support** - the mods works seamlessly with GeyserMC + Floodgate, with the Discord commands updating automatically to accept Bedrock edition accounts¹

###### ¹ The account must be in the Geyser Global Cache for the username to work, otherwise it requires their Floodgate UUID
###### ² Player Roles support is only available since version 2.0

This mod works on top of the vanilla whitelist and requires it to be enabled, you can do that by changing `white-list` in `server.properties` to `true` or by running `whitelist on` in the server console

**You can customize the bot messages with a <u>datapack</u>, more about it can be found <u>[here](https://docs.awakenedredstone.com/minecraft/autowhitelist/advanced/custom-messages)</u>**

[![You can find a detailed documentation here](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg)](https://docs.awakenedredstone.com/minecraft/autowhitelist/install)
