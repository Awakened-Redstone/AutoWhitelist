package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.server.PlayerManager.WHITELIST_FILE;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow @Final @Mutable private Whitelist whitelist;

    @Inject(method = "<init>", at = @At(value = "TAIL"), require = 1, remap = false)
    private void modifyWhitelist(CallbackInfo ci) {
        AutoWhitelist.LOGGER.debug("Replaced whitelist");
        whitelist = new ExtendedWhitelist(WHITELIST_FILE);
    }
}
