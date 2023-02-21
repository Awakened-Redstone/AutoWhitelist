**Compatibility versions can be found at https://github.com/Awakened-Redstone/AutoWhitelist/releases**
<br/>
AutoWhitelist is a mod made to automate the whitelisting of players based on their Discord role.  
Its main purpose is to make the whitelisting of Twitch subscribers and Youtube channel members easy.
<br/>
It has also been made to be extremely stable so it can be used on servers that are constantly updating to the latest snapshot, so it is dependency free.
<br/>
<br/>

#### You can set the bot message text with a <u>datapack</u>, a template datapack can be found <u>[here](https://example.com)</u>

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

Inside the `whitelist` entry you will find, by default, `"tier1": []`, where is `tier1` you will put an existing Minecraft team, you can create one with `/team add teamNameHere`  
Inside the array (`[]`) you will put the ID of the roles that will be assigned to that team, please note that the role ID has to be as a String (`"roleIdHere"`)  
The `owners` entry is an option that allows the users in it to use the developer commands. In the option you put the ID of the users you want to have access to the dev commands.
</details>

<details>
<summary>You can find an example configuration file here</summary>

```json
{
    "version": 2.1,
    "whitelistScheduledVerificationSeconds": 30,
    "prefix": "np!",
    "token": "OTM3ODgwNjU3Njk3MzA4Njgy.YfiLbQ.9xiSPQNQRaq2tYKhbjJIBzqqlis",
    "clientId": "937880657697308682",
    "discordServerId": "387760260166844418",
    "whitelist": {
        "subTier1": [
            "744941527545020468"
	]
    },
    "owners": ["387745099204919297", "483715272960901120"]
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