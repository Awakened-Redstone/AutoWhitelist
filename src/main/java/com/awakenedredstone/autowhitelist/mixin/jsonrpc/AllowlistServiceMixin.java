package com.awakenedredstone.autowhitelist.mixin.jsonrpc;

import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerDto;
import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerProfile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftPlayerListService;
import net.minecraft.server.jsonrpc.methods.AllowlistService;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(AllowlistService.class)
public class AllowlistServiceMixin {
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/jsonrpc/internalapi/MinecraftPlayerListService;getUser(Ljava/util/Optional;Ljava/util/Optional;)Ljava/util/concurrent/CompletableFuture;"), method = {
      "method_73831", "method_73825", "method_73836"
    })
    private static CompletableFuture<Optional<NameAndId>> linkList(MinecraftPlayerListService instance, Optional<UUID> uuid, Optional<String> playerName, Operation<CompletableFuture<Optional<NameAndId>>> original, @Local(argsOnly = true) PlayerDto playerDto) {
        CompletableFuture<Optional<NameAndId>> future = original.call(instance, uuid, playerName);
        LinkedPlayerDto linkedDto = (LinkedPlayerDto) playerDto;

        if (!linkedDto.isLinked()) return future;

        return future.thenApply(optional -> {
            if (optional.isEmpty()) return optional;

            NameAndId nameAndId = optional.get();
            //noinspection OptionalGetWithoutIsPresent
            return Optional.of(new LinkedPlayerProfile(nameAndId.id(), nameAndId.name(), linkedDto.role().get(), linkedDto.discordId().get(), linkedDto.lockedUntil().orElse(-1L)));
        });
    }
}
