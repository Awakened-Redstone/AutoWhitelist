package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.server.PlayerManager.WHITELIST_FILE;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Mutable @Final @Shadow private final Whitelist whitelist = new ExtendedWhitelist(WHITELIST_FILE);
}
