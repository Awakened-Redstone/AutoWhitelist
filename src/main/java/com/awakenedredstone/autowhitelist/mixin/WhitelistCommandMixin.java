package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
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

    //? if <1.21.9 {
    /*@WrapOperation(method = "executeRemove", at = @At(value = "NEW", target = "(Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/server/WhitelistEntry;"))
    *///?} else {
    @WrapOperation(method = "removePlayers", at = @At(value = "NEW", target = "(Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/server/players/UserWhiteListEntry;"))
    //?}
    private static UserWhiteListEntry useExtendedWhitelist(/*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile, Operation<UserWhiteListEntry> original, @Local UserWhiteList whitelist) {
        return ((LinkingWhitelist) whitelist).getOrCreateEntry(profile);
    }

    @WrapWithCondition(method = "removePlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"))
    private static boolean failMessageOnBadEntry(CommandSourceStack source, Supplier<Component> feedbackSupplier, boolean broadcastToOps, @Local UserWhiteList whitelist, @Local /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId gameProfile) {
        return !whitelist.isWhiteListed(gameProfile);
    }

    //? if <1.21.9 {
    /*@Inject(method = "executeRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Whitelist;remove(Lnet/minecraft/server/ServerConfigEntry;)V", shift = At.Shift.AFTER))
    *///?} else {
    @Inject(method = "removePlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/UserWhiteList;remove(Lnet/minecraft/server/players/StoredUserEntry;)Z", shift = At.Shift.AFTER))
    //?}
    private static void removeWhitelistCache(CallbackInfoReturnable<Integer> cir, @Local UserWhiteList whitelist, @Local /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId gameProfile) {
        if (!whitelist.isWhiteListed(gameProfile)) {
            WhitelistHandler.getWhitelist().getCache().remove(gameProfile);
        }
    }

    @Definition(id = "i", local = @Local(type = int.class))
    @Expression("i = i + @(1)")
    @ModifyExpressionValue(method = "removePlayers", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static int stopIncrementOnBadEntry(
      int original,
      @Local(argsOnly = true, name = "source") CommandSourceStack source,
      @Local(name = "list") UserWhiteList list,
      @Local(name = "target") /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId target
    ) {
        if (list.isWhiteListed(target)) {
            return original - 1;
        }

        return original;
    }
}
