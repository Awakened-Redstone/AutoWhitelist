package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.ServerConfigList;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class WhitelistCache extends ServerConfigList<ExtendedGameProfile, WhitelistCacheEntry> {
    public WhitelistCache(File file) {
        super(file);
    }

    @Override
    protected ServerConfigEntry<ExtendedGameProfile> fromJson(JsonObject json) {
        return new WhitelistCacheEntry(json);
    }

    @Override
    protected String toString(ExtendedGameProfile gameProfile) {
        return gameProfile.getId().toString();
    }

    @Override
    public void add(WhitelistCacheEntry entry) {
        if (AutoWhitelist.CONFIG.enableWhitelistCache()) super.add(entry);
    }

    @Nullable
    public WhitelistCacheEntry get(GameProfile key) {
        return super.get(new ExtendedGameProfile(key.getId(), key.getName(), null, null));
    }

    public void remove(GameProfile key) {
        super.remove(new ExtendedGameProfile(key.getId(), key.getName(), null, null));
    }
}
