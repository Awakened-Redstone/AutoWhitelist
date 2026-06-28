package com.awakenedredstone.autowhitelist.jsonrpc;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;

public class AutoWhitelistJsonRpc {
    public static void registerIncoming() {
        var registry = BuiltInRegistries.INCOMING_RPC_METHOD;

        IncomingRpcMethod.method(AutoAllowlistService::register)
          .description("Register players to the allowlist")
          .param("user", AutoWhitelistSchema.LINKED_PLAYER_SCHEMA.asArray())
          .response("allowlist", AutoWhitelistSchema.LINKED_PLAYER_SCHEMA.asArray())
          .register(registry, AutoWhitelist.id("allowlist/register"));
    }
}
