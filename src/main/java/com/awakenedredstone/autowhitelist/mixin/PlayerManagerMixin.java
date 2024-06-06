package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import org.objectweb.asm.Opcodes;
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

    @Inject(method = {
      "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/class_5455$class_6890;Lnet/minecraft/class_29;I)V",
      "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/class_7780;Lnet/minecraft/class_29;I)V"
    },
      at = @At(value = "FIELD", target = "Lnet/minecraft/server/PlayerManager;whitelist:Lnet/minecraft/server/Whitelist;", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD , remap = true),
      require = 0, remap = false)
    private void whitelist(CallbackInfo ci) {
        whitelist = new ExtendedWhitelist(WHITELIST_FILE);
    }
}
