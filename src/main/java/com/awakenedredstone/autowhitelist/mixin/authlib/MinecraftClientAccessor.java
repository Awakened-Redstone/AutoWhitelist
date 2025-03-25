package com.awakenedredstone.autowhitelist.mixin.authlib;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

@Mixin(value = MinecraftClient.class, remap = false)
public interface MinecraftClientAccessor {
    @Accessor("accessToken") String autowhitelist$getAccessToken();
    @Accessor("proxy") Proxy autowhitelist$getProxy();

    @Invoker HttpURLConnection callCreateUrlConnection(final URL url);
}
