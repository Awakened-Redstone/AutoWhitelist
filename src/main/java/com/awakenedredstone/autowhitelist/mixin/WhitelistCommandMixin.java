package com.awakenedredstone.autowhitelist.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    @WrapWithCondition(method = "executeRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/ServerCommandSource;sendFeedback(Ljava/util/function/Supplier;Z)V"))
    private static boolean failOnBadEntry(ServerCommandSource instance, Supplier<Text> feedbackSupplier, boolean broadcastToOps, @Local Whitelist whitelist, @Local int i, @Local GameProfile gameProfile) {
        if (whitelist.isAllowed(gameProfile)) {
            i--;
            return false;
        }

        return true;
    }
}
