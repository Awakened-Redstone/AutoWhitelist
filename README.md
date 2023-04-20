### You can find the lite version at https://modrinth.com/mod/autowhitelist-lite

**The mod may not work properly with other discord integration mods if they use a different version of JDA**
<br/>
AutoWhitelist is a mod made to automate the whitelisting of players based on their Discord role.  
Its main purpose is to make the whitelisting of Twitch subscribers and Youtube channel members easy.
<br/>
It has also been made to be extremely stable so it can be used on servers that are constantly updating to the latest snapshot, so it is dependency free.
<br/>
<br/>

#### You can set the bot message text with a <u>datapack</u>, more about it can be found <u>[here](https://github.com/NucleoidMC/Server-Translations#usage)</u>

Configuring the mod can be a bit complicated, you can find a tutorial below.
<br/>
<br/>

<details>
<summary>Creating the bot</summary>

![Creating the bot](https://cdn.glitch.global/b4fe08b2-a216-4ca6-a836-38b072b573c1/create_bot.gif)
</details>

<details>
<summary>Important settings</summary>

![Important settings](https://cdn.glitch.global/b4fe08b2-a216-4ca6-a836-38b072b573c1/bot_sttings.gif)
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

![Adding to your server](https://cdn.glitch.global/0efc937f-9a16-4877-a535-7e0839b5f61c/Peek%202022-06-17%2022-44.gif?v=1655516745674)
</details>

<details>
<summary>Final steps</summary>

On the config file, `entries` will be empty by default, there you will configure what the server will do when whitelisting the players.
There are **4** types of entries, for vanilla you have `TEAM` and `COMMAND`, if you have luckperms you can also use `LUCKPERMS_GROUP` and `LUCKPERMS_PERMISSION`.
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
    "addCommand": "/pardon %player%",
    "removeCommand": "/ban %player%"
}
```
</details>
<details>
<summary>Luckperms examples</summary>

<details>
<summary>Group examples</summary>

```json5
{
    "roleIds": ["Discord role id"],
    "type": "LUCKPERMS_GROUP",
    "group": "TIER_1"
}
```
</details>
<details>
<summary>Permission examples</summary>

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

To register your player run the command `register <username>`, by default the prefix is `np!`  
The command requires the user to insert their Java username, if they change their nick, they don't have to register again.
There is no way for a user to change their registered account or to register another one, a moderator will have to run `/whitelist remove <player>` in the Minecraft server.
An example of running the command is `np!register AwakenedRedstone`  
The mod does not support offline servers, this means, it only registers official Minecraft Java Edition accounts.
<br/>  
To reload settings run `/autowhitelist reload [bot|config]` to reload only one thing or `/autowhitelist reload` to reload everything

<br/>

![No froge](https://i.ibb.co/yphNcXz/fabric-only-banner.png)