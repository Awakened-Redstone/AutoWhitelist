package com.awakenedredstone.autowhitelist.mixin.authlib;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.HttpURLConnection;
import java.net.URL;

@Mixin(value = MinecraftClient.class, remap = false)
public interface MinecraftClientAccessor {
    @Invoker HttpURLConnection callCreateUrlConnection(final URL url);
}
