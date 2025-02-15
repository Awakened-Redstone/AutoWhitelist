package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {

    @Expression("'commands.whitelist.remove.failed'")
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private static String replaceString(String original) {
        return "commands.autowhitelist.remove.failed";
    }

    @WrapOperation(method = "executeRemove", at = @At(value = "NEW", target = "(Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/server/WhitelistEntry;"))
    private static WhitelistEntry useExtendedWhitelist(GameProfile profile, Operation<WhitelistEntry> original, @Local Whitelist whitelist) {
        return ((ExtendedWhitelist) whitelist).getEntry(profile);
    }

    @WrapWithCondition(method = "executeRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/ServerCommandSource;sendFeedback(Ljava/util/function/Supplier;Z)V"))
    private static boolean failMessageOnBadEntry(ServerCommandSource source, Supplier<Text> feedbackSupplier, boolean broadcastToOps, @Local Whitelist whitelist, @Local GameProfile gameProfile) {
        return !whitelist.isAllowed(gameProfile);
    }

    @Inject(method = "executeRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Whitelist;remove(Lnet/minecraft/server/ServerConfigEntry;)V", shift = At.Shift.AFTER))
    private static void removeWhitelistCache(CallbackInfoReturnable<Integer> cir, @Local Whitelist whitelist, @Local GameProfile gameProfile) {
        if (!whitelist.isAllowed(gameProfile)) {
            AutoWhitelist.WHITELIST_CACHE.remove(gameProfile);
        }
    }

    @Definition(id = "i", local = @Local(type = int.class))
    @Expression("i = i + @(1)")
    @ModifyExpressionValue(method = "executeRemove", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static int stopIncrementOnBadEntry(int original, @Local(argsOnly = true) ServerCommandSource source, @Local Whitelist whitelist, @Local GameProfile gameProfile) {
        if (whitelist.isAllowed(gameProfile)) {
            return 0;
        }

        return original;
    }
}
