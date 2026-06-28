package com.awakenedredstone.autowhitelist.mixin.jsonrpc;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.server.jsonrpc.api.Schema;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Debug(export = true)
@Mixin(Schema.class)
public class SchemaMixin {
    @Definition(id = "PLAYER_SCHEMA", field = "Lnet/minecraft/server/jsonrpc/api/Schema;PLAYER_SCHEMA:Lnet/minecraft/server/jsonrpc/api/SchemaComponent;")
    @Expression("PLAYER_SCHEMA = @(?)")
    @ModifyArg(at = @At(value = "MIXINEXTRAS:EXPRESSION"), method = "<clinit>")
    private static Schema<?> linkedSchema(Schema<?> schema) {
        return schema.withField("discordId", Schema.STRING_SCHEMA).withField("role", Schema.STRING_SCHEMA).withField("lockedUntil", Schema.INT_SCHEMA);
    }
}
