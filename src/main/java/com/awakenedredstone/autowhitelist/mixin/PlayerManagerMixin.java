package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.QueryEventHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.string.Texts;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.MutableComponent;
/*? if >=1.21.9 {*/ import net.minecraft.server.notifications.NotificationService; /*?}*/
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Shadow @Final @Mutable private UserWhiteList whitelist;
    @Shadow @Final public static File WHITELIST_FILE;

    @Inject(method = "<init>", at = @At(value = "TAIL"), require = 1, remap = false)
    private void modifyWhitelist(CallbackInfo ci /*? if >=1.21.9 {*/, @Local(argsOnly = true) NotificationService managementListener /*?}*/) {
        whitelist = new LinkingWhitelist(WHITELIST_FILE/*? if >=1.21.9 {*/, managementListener /*?}*/);
        AutoWhitelist.LOGGER.debug("Replaced whitelist");
    }

    @Definition(id = "translatable", method = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;")
    @Expression("translatable('multiplayer.disconnect.not_whitelisted')")
    @WrapOperation(method = "canPlayerLogin", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private static MutableComponent appendMessage(String key, Operation<MutableComponent> original) {
        String suffix = AutoWhitelist.useGuyser() ? ".geyser" : "";
        MutableComponent originalText = original.call(key);
        if (!QueryEventHandler.NOT_WHITELISTED_MESSAGE.isLocked()) {
            QueryEventHandler.NOT_WHITELISTED_MESSAGE.setAndLock(originalText.getContents());
        }
        return originalText.append("\n" + Texts.translated("multiplayer.autowhitelist.disconnect.not_whitelisted.tip" + suffix));
    }
}
