## Protocol definitions

### Conventions and Terminology

The keywords "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT","SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT
RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described
in [BCP 14](https://datatracker.ietf.org/doc/html/bcp14) [[RFC2119]](https://datatracker.ietf.org/doc/html/rfc2119) [[RFC8174]](https://datatracker.ietf.org/doc/html/rfc8174)
when, and only when, they appear in all capitals, as shown here.

The following terms are used:

| Term             | Definition                                                                                                                                                    |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| client           | The servers that receive the sync events from the Server, also the endpoint initiating the connection                                                         |
| server           | The main Minecraft server handling the Discord events and doing the sync checks and link validations, also the endpoint that did not initiate the connection. |
| endpoint         | Either the client or server of the connection.                                                                                                                |
| multilink        | The name for the system/network that enables AutoWhitelist to operate on various servers with a central Minecraft server                                      |
| Minecraft server | The Java based application that handles incoming and outcoming connections and the backend for the endpoint                                                   |
| connection       | A transport-layer connection between two endpoints.                                                                                                           |
| multilink secret | A fixed shared scret set in the configuration files.                                                                                                          |
| race condition   | A scenario where data is received while the application is in between states or arrived at a delay where it may cause unexpected results.                     |

### Awareness

The server is not aware of who are the clients, the client connects to the server and authenticates with the shared secret.

### Security considerations

The connection MUST enable encryption at the start of the login process.

The multilink secret SHALL NOT be directly sent over the network, instead a challenge that requires it should be
completed. A valid challenge is requiring the combination of the multilink secret with a random sequence known by both
ends and provide the resulting hash

## Race conditions

Scenarios of race condition MUST be considered and properly handled.

The handling for those scenarios within AutoWhitelist's implementation of multilink are defined below.

### Config update

Most of the config is per server and independent, except for entry actions, which should by synced to the server. \
The actions are all operated around RoleEntryMap, which should only be updated after receiving the resolved roles from
the server.

### Whitelist update

The whitelist race-condition, where the server and client are temporarily desynced, is not a concern, as the client will
adequately handle the events.

### Login with cache

The login with the cache receives the discord data asynchronously, and should check if the user was whitelisted while it
fetched the data before attempting to whitelist them.

### Discord commands

The change of state while those are built is not handled, and will show as is, unless a concern or issue emerges from
this structure

## Protocol sequences

### Login:

```sequence
define Server as S
define Client as C

C -> S: Multilink Handshake
S -> C: Encryption request (same as vanilla, integer id is `0x00`, `should_authenticate` is always false)
C -> S: Encryption response (same as vanilla)
state over S, C: Both enable encryption
S -> C: Request secret
C -> S: Verification secret
S -> C: Authorized      <-- Current dev stage
divider tear with height 20: Server requests Config sync
divider tear with height 20: Server request Link status sync
```

### Config sync:

```sequence
define Server as S
define Client as C

S -> C: Link config request (only sent if the server wants an update)
C -> S: Link config response
S -> C: Resolved roles
```

### Whitelist sync:

```sequence
define Server as S
define Client as C

S -> C: Whitelist sync request (only sent if the server wants an update)
C -> S: Whitelist sync response
```

### Change event:

```sequence
define Server as S
define Client as C

S -> C: Change notification
C -> S: Change response
```

### Discord data:

```sequence
C -> S: Discord data request
S -> C: Discord data response
```

# Packet specification

:::note
The packet integer ID is fully handled by Minecraft, and so it is not a concern if another mod causes it to change
:::

[//]: # (----------------------------------------------------------------------------------------------------------------)

## Serverbound

### Handshake

This packet causes the connection to switch into multilink. It should be sent right after opening the TCP connection to
prevent the server from disconnecting. This is at the same stage as the vanilla handshake. \
The server switches to the Login state once this packet is received.

| Packet Type ID                                                              | State       | Bound To | Field Name | Field Type | Notes |
|-----------------------------------------------------------------------------|-------------|----------|------------|------------|-------|
| Integer id: <br/>`0x01`¹<br/><br/>Identifier:<br/>`autowhitelist:handshake` | Handshaking | Server   | No fields  | <@         | <@    |

###### ¹ The value is for a server with only AutoWhitelist and it's dependencies. It may be different when other mods are present

### Encryption response

This is the same as the vanilla packet, except for the id, you can find it in
the [Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_protocol/Packets#Encryption_Response)

The integer ID is 0, and the identifier is `autowhitelist:encrypt_response`

### Verification secret

This packet must be sent after enabling encryption to validate that the connected client can be trusted and is part of
the multilink network. The secret is never sent, instead it is combined with the challenge and the resulting SHA1 is
sent.

| Packet Type ID                                                         | State | Bound To | Field Name | Field Type             | Notes                                                               |
|------------------------------------------------------------------------|-------|----------|------------|------------------------|---------------------------------------------------------------------|
| Integer id:<br/>`0x01`<br/><br/>Identifier:<br/>`autowhitelist:secret` | Login | Server   | Token      | Prefixed Array of Byte | The hashed token generated from the key and the challange           |
| \                                                                      | \     | \        | Name       | String (255)           | The name for the server, used to display on Discord command results |

### Link config response

This provides the required details to know if a user can be whitelisted or not. \
The options are represented in order, where the index in the array is it's position in the list in the config.

| Packet Type ID                                                                  | State | Bound To | Field Name | Field Type                   | Notes                                                                |
|---------------------------------------------------------------------------------|-------|----------|------------|------------------------------|----------------------------------------------------------------------|
| Integer id:<br/>`0x00`<br/><br/>Identifier:<br/>`autowhitelist:config_response` | Play  | Server   | Entries    | Jagged Array of String (127) | An ordered array of roles, they may be numeric or `@` prefixed names |

### Whitelist sync response

| Packet Type ID                                                                    | State | Bound To | Field Name | Field Type                        | Notes                                                         |
|-----------------------------------------------------------------------------------|-------|----------|------------|-----------------------------------|---------------------------------------------------------------|
| Integer id:<br/>`0x01`<br/><br/>Identifier:<br/>`autowhitelist:whitelist_respone` | Play  | Server   | Entries    | Prefixed Array of Whitelist Entry | An array containing what is virtually a copy of the whitelist |

### Change response

| Packet Type ID                                                                  | State | Bound To | Field Name | Field Type | Notes                                     |
|---------------------------------------------------------------------------------|-------|----------|------------|------------|-------------------------------------------|
| Integer id:<br/>`0x02`<br/><br/>Identifier:<br/>`autowhitelist:change_received` | Play  | Server   | Reply      | Identifier | The reply identifier for the notification |

### Discord data request

| Packet Type ID                                                                   | State | Bound To | Field Name | Field Type | Notes                                |
|----------------------------------------------------------------------------------|-------|----------|------------|------------|--------------------------------------|
| Integer id: <br/>`0x03`<br/><br/>Identifier:<br/>`autowhitelist:discord_request` | Play  | Server   | User       | Long       | The Snowflake ID of the Discord user |

[//]: # (--------------------------------------------------------------------------------------)

## Clientbound

### Encryption request

This is the same as the vanilla packet, except for the identifier, you can find it in
the [Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_protocol/Packets#Encryption_Request).

The Server ID field is always empty and the `Should authenticate` field is always `false`

The integer ID is 0, and the identifier is `autowhitelist:encrypt`

### Secret request

| Packet Type ID                                                                | State | Bound To | Field Name | Field Type | Notes |
|-------------------------------------------------------------------------------|-------|----------|------------|------------|-------|
| Integer id: <br/>`0x01`<br/><br/>Identifier:<br/>`autowhitelist:authenticate` | Login | Client   | No fields  | <@         | <@    |

### Link config request

| Packet Type ID                                                                  | State | Bound To | Field Name | Field Type | Notes |
|---------------------------------------------------------------------------------|-------|----------|------------|------------|-------|
| Integer id: <br/>`0x00`<br/><br/>Identifier:<br/>`autowhitelist:config_request` | Play  | Client   | No fields  | <@         | <@    |

### Resolved roles

| Packet Type ID                                                                  | State | Bound To | Field Name | Field Type           | Notes                                                                                                          |
|---------------------------------------------------------------------------------|-------|----------|------------|----------------------|----------------------------------------------------------------------------------------------------------------|
| Integer id: <br/>`0x01`<br/><br/>Identifier:<br/>`autowhitelist:mapped_entries` | Play  | Client   | No fields  | Jagged Array of Long | An ordered array of the parsed roles, they are all [Snowflake ID](https://en.wikipedia.org/wiki/Snowflake_ID)s |

### Whitelist sync request

| Packet Type ID                                                                     | State | Bound To | Field Name | Field Type | Notes |
|------------------------------------------------------------------------------------|-------|----------|------------|------------|-------|
| Integer id: <br/>`0x02`<br/><br/>Identifier:<br/>`autowhitelist:whitelist_request` | Play  | Client   | No fields  | <@         | <@    |

### Change notification

| Packet Type ID                                                                       | State | Bound To | Field Name | Field Type | Notes |
|--------------------------------------------------------------------------------------|-------|----------|------------|------------|-------|
| Integer id: <br/>`0x03`<br/><br/>Identifier:<br/>`autowhitelist:change_notification` | Play  | Client   | No fields  | <@         | <@    |

### Discord data response

| Packet Type ID                                                                    | State | Bound To | Field Name | Field Type | Notes                                                 |
|-----------------------------------------------------------------------------------|-------|----------|------------|------------|-------------------------------------------------------|
| Integer id: <br/>`0x04`<br/><br/>Identifier:<br/>`autowhitelist:discord_response` | Play  | Client   | Role       | Long       | The Snowflake ID of the qualified role, or -1 if none |

-------------------------------------------------------------------------------

## Protocol data format

| Name              | Size (bytes) | Encodes                               | Notes                                                          |
|-------------------|--------------|---------------------------------------|----------------------------------------------------------------|
| Jagged Array of X | Varies       | Prefixed Array of Prefixed Array of X | A jagged array of X, encoded using Prefixed Arrays recursively |
| Whitelist Entry   | Varies       | See Whitelist Entry format            |                                                                |
| Notification Data | Varies       | See Notification Data format          |                                                                |

### Whitelist Entry format

| Name      | Type                      | Notes                                                                   |
|-----------|---------------------------|-------------------------------------------------------------------------|
| name      | String (16)               | The username for the player                                             |
| uuid      | UUID                      | The UUID for the player                                                 |
| discordId | Prefixed Optional of Long | The associated Discord ID for the player (absent if not a linked entry) |
| role      | Optional of Long          | The current Role ID for the player (absent if discordId is absent)      |
| lock      | Optional of Long          | The timestamp for the lock (only absent if discordId is absent)         |

### Notification Data format

| Name      | Type                      | Notes                                                                   |
|-----------|---------------------------|-------------------------------------------------------------------------|
| discordId | Prefixed Optional of Long | The associated Discord ID for the player (absent if not a linked entry) |
| role      | Optional of Long          | The current Role ID for the player (absent if discordId is absent)      |
| lock      | Optional of Long          | The timestamp for the lock (only absent if discordId is absent)         |
