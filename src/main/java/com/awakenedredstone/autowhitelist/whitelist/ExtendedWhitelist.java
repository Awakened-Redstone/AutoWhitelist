package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExtendedWhitelist extends Whitelist {

    public ExtendedWhitelist(File file) {
        super(file);
    }

    @SuppressWarnings("ConstantConditions")
    protected ServerConfigEntry<GameProfile> fromJson(JsonObject json) {
        ExtendedWhitelistEntry entry = new ExtendedWhitelistEntry(json);
        try {
            if (((ServerConfigEntryMixin<?>) entry).getKey() != null) return entry;
            else return new WhitelistEntry(json);
        } catch (ClassCastException e) {
            return new WhitelistEntry(json);
        }
    }

    public boolean isAllowed(ExtendedGameProfile profile) {
        return this.contains(profile);
    }

    protected String toString(ExtendedGameProfile gameProfile) {
        return gameProfile.getId().toString();
    }

    public JsonObject fromProfile(ExtendedWhitelistEntry entry) {
        JsonObject json = new JsonObject();
        entry.write(json);
        return json;
    }

    public Collection<? extends WhitelistEntry> getEntries() {
        return this.values();
    }


    public void remove(String var5, Type type) {
        switch (type) {
            case DISCORD_ID -> values().stream().filter(entry -> {
                try {
                    return ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId().equals(var5);
                } catch (ClassCastException exception) {
                    return false;
                }
            }).forEach(whitelistEntry -> remove((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) whitelistEntry).getKey()));
            case USERNAME -> values().stream().filter(entry -> {
                return ((GameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getName().equals(var5);
            }).forEach(whitelistEntry -> remove((GameProfile) ((ServerConfigEntryMixin<?>) whitelistEntry).getKey()));
        }
    }

    public List<ExtendedGameProfile> getProfilesFromDiscordId(String id) {
        return values().stream().filter(entry -> {
            try {
                return ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId().equals(id);
            } catch (ClassCastException exception) {
                return false;
            }
        }).map(whitelistEntry -> (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) whitelistEntry).getKey()).toList();
    }

    public List<ExtendedWhitelistEntry> getFromDiscordId(String id) {
        return values().stream()
                .filter(entry -> entry instanceof ExtendedWhitelistEntry)
                .filter(entry -> ((ServerConfigEntryMixin<?>) entry).getKey() instanceof ExtendedGameProfile profile && profile.getDiscordId().equals(id))
                .map(v -> (ExtendedWhitelistEntry) v)
                .toList();
    }

    @Nullable
    public GameProfile getFromUsername(String var5) {
        return values().stream()
                .filter(entry -> ((GameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getName().equals(var5))
                .map(whitelistEntry -> (GameProfile) ((ServerConfigEntryMixin<?>) whitelistEntry).getKey())
                .findFirst().orElse(null);
    }

    public enum Type {
        DISCORD_ID,
        USERNAME
    }
}
