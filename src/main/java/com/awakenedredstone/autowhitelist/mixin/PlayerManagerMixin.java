package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.duck.WhitelistCacheHolder;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCache;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
/*? if >=1.21.9 {*/ import net.minecraft.server.dedicated.management.listener.ManagementListener; /*?}*/
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.server.PlayerManager.WHITELIST_FILE;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin implements WhitelistCacheHolder {
    @Shadow @Final @Mutable private Whitelist whitelist;
    @Unique private WhitelistCache whitelistCache;

    @Inject(method = "<init>", at = @At(value = "TAIL"), require = 1, remap = false)
    private void modifyWhitelist(CallbackInfo ci /*? if >=1.21.9 {*/, @Local(argsOnly = true) ManagementListener managementListener /*?}*/) {
        whitelist = new ExtendedWhitelist(WHITELIST_FILE/*? if >=1.21.9 {*/, managementListener /*?}*/);
        whitelistCache = new WhitelistCache(AutoWhitelist.WHITELIST_CACHE_FILE/*? if >=1.21.9 {*/, managementListener /*?}*/);
        AutoWhitelist.LOGGER.debug("Replaced whitelist");
    }

    @Override
    public WhitelistCache autoWhitelist$getWhitelistCache() {
        return whitelistCache;
    }
}
