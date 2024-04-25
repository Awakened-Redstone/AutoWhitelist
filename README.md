[<img src="https://storage.ko-fi.com/cdn/brandasset/kofi_button_blue.png" width="300px"/>](https://ko-fi.com/awakenedredstone)

![fabric-api](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg)

<br/>

**The mod may not work properly with other discord integration mods if they use a different version of JDA**
<br/>

AutoWhitelist is a mod made to automate the whitelisting of players based on their Discord role.  
Its main purpose is to make the whitelisting of Twitch subscribers and YouTube channel members easy.
<br/>
<br/>
This mod works on top of the vanilla whitelist, so make sure to enable the vanilla whitelist, you can do that by changing `white-list` on `server.properties` to `true` or by running `whitelist on` on the server console
<br/>
<br/>

#### The mod does not support running the register command on DMs!

#### You can set the bot message text with a <u>datapack</u>, more about it can be found <u>[here](https://github.com/Awakened-Redstone/AutoWhitelist/wiki/Custom-messages)</u>

## You can find a tutorial on how to configure and install the mod [here](https://docs.awakenedredstone.com/minecraft/autowhitelist/install)
### The configuration file can be found at `config/autowhitelist.json5`, it is auto-generated after the mod is run for the first time
<br/>

To register your player run the command `register <username>`, by default the prefix is `np!`  
The command requires the user to insert their Java username, if they change their nick, they don't have to register again.
There is no way for a user to change their registered account or to register another one, a moderator will have to run `/whitelist remove <player>` in the Minecraft server.
An example of running the command is `np!register AwakenedRedstone`  
The mod does not officially support offline servers.  
When a player looses the role or leaves the discord server they are automatically removed from the whitelist
<br/>  
To reload settings run `/autowhitelist reload [bot|config]` to reload only one thing or `/autowhitelist reload` to reload everything

### About porting
Porting to other platforms is fully allowed, as long as:
1. A link to the original mod's Modrinth page is put within the first c5 lines of the project description
2. Credits are given to the author of the original mod
3. The ported version is free and open source
4. The config structure allows a seamless or easy migration from/to the original mod

<br/>

A _native_ Forge version will **not** be made, but I <u>**may**</u> add compatibility for [Connector](https://modrinth.com/mod/connector) if required  
![No froge](https://i.ibb.co/yphNcXz/fabric-only-banner.png)
