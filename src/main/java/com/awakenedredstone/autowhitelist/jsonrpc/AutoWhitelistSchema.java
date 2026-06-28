package com.awakenedredstone.autowhitelist.jsonrpc;

import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerDto;
import net.minecraft.server.jsonrpc.api.ReferenceUtil;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.api.SchemaComponent;

public class AutoWhitelistSchema {
    public static final SchemaComponent<LinkedPlayerDto> LINKED_PLAYER_SCHEMA = registerSchema(
      "player", Schema.record(LinkedPlayerDto.CODEC.codec())
        .withField("id", Schema.UUID_SCHEMA)
        .withField("name", Schema.STRING_SCHEMA)
        .withField("discordId", Schema.STRING_SCHEMA)
        .withField("role", Schema.STRING_SCHEMA)
        .withField("lockedUntil", Schema.INT_SCHEMA)
    );

    private static <T> SchemaComponent<T> registerSchema(String name, Schema<T> schema) {
        SchemaComponent<T> schemaComponent = new SchemaComponent<>(name, ReferenceUtil.createLocalReference(name), schema);
        Schema.getSchemaRegistry().add(schemaComponent);
        return schemaComponent;
    }
}
