package com.awakenedredstone.autowhitelist.mixin.jsonrpc;

import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerDto;
import com.awakenedredstone.autowhitelist.server.profile.LinkedNameAndId;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Debug(export = true)
@Mixin(PlayerDto.class)
public class PlayerDtoMixin {
    @Definition(id = "CODEC", field = "Lnet/minecraft/server/jsonrpc/api/PlayerDto;CODEC:Lcom/mojang/serialization/MapCodec;")
    @Expression("CODEC = @(?)")
    @ModifyExpressionValue(at = @At(value = "MIXINEXTRAS:EXPRESSION"), method = "<clinit>")
    private static MapCodec<? extends PlayerDto> fromLinked(MapCodec<PlayerDto> original) {
        return LinkedPlayerDto.CODEC;
    }

    @Inject(at = @At("HEAD"), method = "from(Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/server/jsonrpc/api/PlayerDto;", cancellable = true)
    private static void fromLinked(NameAndId nameAndId, CallbackInfoReturnable<PlayerDto> cir) {
        if (nameAndId instanceof LinkedNameAndId linkedProfile) {
            cir.setReturnValue(new LinkedPlayerDto(
              Optional.of(linkedProfile.id()),
              Optional.of(linkedProfile.name()),
              Optional.of(linkedProfile.getDiscordId()),
              Optional.of(linkedProfile.getRole()),
              Optional.of(linkedProfile.getLockedUntil())
            ));
        }
    }
}
