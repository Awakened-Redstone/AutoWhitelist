[<img src="https://storage.ko-fi.com/cdn/brandasset/kofi_button_blue.png" width="30%"/>](https://ko-fi.com/awakenedredstone)

<br/>

**The mod may not work properly with other discord integration mods if they use a different version of JDA**
<br/>

AutoWhitelist is a mod made to automate the whitelisting of players based on their Discord role.  
Its main purpose is to make the whitelisting of Twitch subscribers and Youtube channel members easy.
<br/>
<br/>
This mod works on top of the vanilla whitelist, so make sure to enable the vanilla whitelist, you can do that by changing `white-list` on `server.properties` to `true` or by running `whitelist on` on the server console
<br/>
<br/>

#### The mod does not support running the register command on DMs!

#### You can set the bot message text with a <u>datapack</u>, more about it can be found <u>[here](https://github.com/Awakened-Redstone/AutoWhitelist/wiki/Custom-messages)</u>

Configuring the mod can be a bit complicated, you can find a tutorial below.
<br/>
The configuration file can be found at `config/autowhitelist.json5`, it is auto-generated after the mod is run for the first time
<br/>
<br/>

<details>
<summary>Creating the bot</summary>

![Creating the bot gif](https://raw.githubusercontent.com/Awakened-Redstone/Awakened-Redstone/master/assets/images/autowhitelist/create_bot_v2.gif)
</details>

<details>
<summary>Important settings</summary>

![Important settings gif](https://raw.githubusercontent.com/Awakened-Redstone/Awakened-Redstone/master/assets/images/autowhitelist/bot_settings_v2.gif)
</details>

<details>
<summary>Getting the token</summary>

![Getting the token](https://cdn.glitch.global/b4fe08b2-a216-4ca6-a836-38b072b573c1/Screenshot_1403.png)
</details>

<details>
<summary>Getting ClientId</summary>

![Getting ClientId](https://cdn.glitch.global/b4fe08b2-a216-4ca6-a836-38b072b573c1/Screenshot_1405.png)
</details>

<details>
<summary>Adding to your server</summary>

![Adding to your server gif](https://github.com/Awakened-Redstone/Awakened-Redstone/blob/master/assets/images/autowhitelist/adding_bot.gif?raw=true)
</details>

<details>
<summary>Final steps</summary>

On the config file, `entries` will be empty by default, there you will configure what the server will do when whitelisting the players.
There are **5** types of entries, for vanilla you have `TEAM` and `COMMAND`, if you have luckperms you can also use `LUCKPERMS_GROUP` and `LUCKPERMS_PERMISSION`.
All of them takes a list of `roleIds` that will be used to whitelist the players, and a `type` that will be used to determine what the server will do when whitelisting the players.
The default format is
```json5
{
    "roleIds": ["Discord role id"],
    "type": "TYPE"
}
```

<details>
<summary>Team example</summary>

```json5
{
    "roleIds": ["Discord role id"],
    "type": "TEAM",
    "team": "minecraft_team_name"
}
```
</details>
<details>
<summary>Command example</summary>

```json5
{
    "roleIds": ["Discord role id"],
    "type": "COMMAND",
    "addCommand": "scorereboard players set %player% cool_people 1",
    "removeCommand": "scorereboard players reset %player% cool_people"
}
```
</details>
<details>
<summary>Whitelist example</summary>
This was added on 1.0.0 Alpha 6!

```json5
{
    "roleIds": ["Discord role id"],
    "type": "WHITELIST" //It does no extra action, it just whitelists the player
}
```
</details>
<details>
<summary>Luckperms examples</summary>

<details>
<summary>Group example</summary>

```json5
{
    "roleIds": ["Discord role id"],
    "type": "LUCKPERMS_GROUP",
    "group": "TIER_1"
}
```
</details>
<details>
<summary>Permission example</summary>

```json5
{
    "roleIds": ["Discord role id"],
    "type": "LUCKPERMS_PERMISSION",
    "permission": "minecraft.command.teleport"
}
```
</details>
</details>

The `admins` entry is an option that allows the users in it to use the developer commands. In the option you put the ID of the users you want to have access to the dev commands.
</details>

<details>
<summary>You can find an example configuration file here</summary>

```json5
{
	// No touchy!
	"devVersion": false,
	// When enabled it will keep a cache of previous registered users and will use it to automatically add the user back (if they have the proper role)
	"enableWhitelistCache": true,
	// The period the mod looks for outdated and invalid entries, this is an extra action to guarantee everything is updated
	"updatePeriod": 60,
	// A list of ids to allow users to use the debug commands
	"admins": [
		"387745099204919297",
		"483715272960901120",
		"302481489897979905"
	],
	// The activity shown on the bot status
	"botActivityType": "PLAYING",
	// The bot command prefix
	"prefix": "np!",
	// Your bot token. Never share it, anyone with it has full control of the bot
	"token": "NEVER SHARE YOUR BOT TOKEN",
	"clientId": "937880657697308682",
	"discordServerId": "894529860145920118",
	// The whitelist entry settings, please refer to the documentation to set them up
	"entries": [
		{
			"roleIds": [
				"744941527545020468"
			],
			"type": "TEAM",
			"team": "team1"
		}
	]
}
```
</details>

<br/>

### IMPORTANT: When using multiple roles, their position on the server will matter!
The higher role on the Discord server has higher priority, and so a user with multiple roles will be put on a team associated with the highest role.

<br/>

Possible activity values are:
- NONE (Doesn't change the bot status)
- RESET (Resets the bot status)
- PLAYING (Sets the bot status to "Playing ...")
- STREAMING (Sets the bot status to "Streaming ..."
- LISTENING (Sets the bot status to "Listening to ...")
- WATCHING (Sets the bot status to "Watching ...")

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
1. A link to the original Modrinth page is put within the first 5 lines of the project description
2. Credits are given to the original author
3. The ported version is free and open source
4. The config structure allows a seamless or easy migration from/to the original version


<br/>

A native Forge version will **not** be made, but I <u>**may**</u> add compatibility with [Connector](https://modrinth.com/mod/connector) if required  
![No froge](https://i.ibb.co/yphNcXz/fabric-only-banner.png)
