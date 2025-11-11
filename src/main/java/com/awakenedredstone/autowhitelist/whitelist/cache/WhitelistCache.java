package com.awakenedredstone.autowhitelist.whitelist.cache;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.ServerConfigList;
/*? if >=1.21.9 {*/ import net.minecraft.server.dedicated.management.listener.ManagementListener; /*?}*/
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class WhitelistCache extends ServerConfigList<LinkedPlayerProfile, WhitelistCacheEntry> {
    public WhitelistCache(File file/*? if >=1.21.9 {*/, ManagementListener managementListener /*?}*/) {
        super(file/*? if >=1.21.9 {*/, managementListener /*?}*/);
    }

    @Override
    protected ServerConfigEntry<LinkedPlayerProfile> fromJson(JsonObject json) {
        return new WhitelistCacheEntry(json);
    }

    @Override
    protected String toString(LinkedPlayerProfile gameProfile) {
        return Stonecutter.profileId(gameProfile).toString();
    }

    @Override
    public /*$ entryPatchReturn >>*/boolean add(WhitelistCacheEntry entry) {
        //? if <1.21.9 {
        /*if (AutoWhitelist.CONFIG.enableWhitelistCache) super.add(entry);
        *///?} else {
        return AutoWhitelist.CONFIG.enableWhitelistCache && super.add(entry);
        //?}
    }

    @Nullable
    public WhitelistCacheEntry get(LinkedPlayerProfile key) {
        return super.get(key);
    }

    public WhitelistCacheEntry getFromId(String id) {
        return super.values().stream().filter(entry -> entry.getProfile().getDiscordId().equals(id)).findFirst().orElse(null);
    }

    @Nullable
    public WhitelistCacheEntry get(GameProfile key) {
        return super.get(new LinkedPlayerProfile(key));
    }

    public void remove(GameProfile key) {
        super.remove(new LinkedPlayerProfile(key));
    }

    //? if >=1.21.9 {
    @Nullable
    public WhitelistCacheEntry get(net.minecraft.server.PlayerConfigEntry key) {
        return super.get(new LinkedPlayerProfile(key));
    }

    public void remove(net.minecraft.server.PlayerConfigEntry key) {
        super.remove(new LinkedPlayerProfile(key));
    }
    //?}
}
