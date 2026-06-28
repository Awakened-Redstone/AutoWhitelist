package com.awakenedredstone.autowhitelist.mixin.jsonrpc;

import com.awakenedredstone.autowhitelist.jsonrpc.internalapi.LinkedAllowListService;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftAllowListServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftApi.class)
public class MinecraftApiMixin {

    @WrapOperation(method = "of", at = @At(value = "NEW", target = "(Lnet/minecraft/server/dedicated/DedicatedServer;Lnet/minecraft/server/jsonrpc/JsonRpcLogger;)Lnet/minecraft/server/jsonrpc/internalapi/MinecraftAllowListServiceImpl;"))
    private static MinecraftAllowListServiceImpl modifyWhitelist(DedicatedServer server, JsonRpcLogger logger, Operation<MinecraftAllowListServiceImpl> original) {
        return new LinkedAllowListService(server, logger);
    }
}
