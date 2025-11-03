package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.google.gson.JsonObject;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
/*? if >=1.21.9 {*/ import net.minecraft.server.dedicated.management.listener.ManagementListener; /*?}*/
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ExtendedWhitelist extends Whitelist {
    public ExtendedWhitelist(File file/*? if >=1.21.9 {*/, ManagementListener managementListener /*?}*/) {
        super(file/*? if >=1.21.9 {*/, managementListener /*?}*/);
    }

    public WhitelistEntry getEntry(/*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile) {
        if (this.isAllowed(profile)) {
            return getEntryFromProfile(profile);
        }

        return new WhitelistEntry(profile);
    }

    @Override
    public /*$ entryPatchReturn >>*/boolean remove(/*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry key) {
        /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile;
        if (key instanceof ExtendedPlayerProfile extendedPlayerProfile) {
            profile = extendedPlayerProfile;
        } else if (Stonecutter.profileId(key).getMostSignificantBits() == 0) {
            /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry storedProfile = getProfileFromUUID(Stonecutter.profileId(key));
            if (storedProfile instanceof ExtendedPlayerProfile extendedPlayerProfile) {
                profile = extendedPlayerProfile;
            } else {
                profile = key;
            }
        } else {
            profile = key;
        }

        if (profile instanceof ExtendedPlayerProfile extendedProfile) {
            BaseEntryAction entryAction = RoleActionMap.getNullable(extendedProfile.getRole());
            if (entryAction != null) {
                if (entryAction.isValid()) {
                    entryAction.removeUser(extendedProfile);
                } else {
                    AutoWhitelist.LOGGER.error("Failed to remove {} from the whitelist due to the entry {} not being valid", Stonecutter.profileName(profile), entryAction);
                    AutoWhitelist.getCommandSource().sendFeedback(() -> Text.literal("Failed to remove player from whitelist. Check the server logs for more details."), true);
                    return /*? if >=1.21.9 {*/false/*?}*/;
                }
            }
        }

        //? if <1.21.9 {
        /*super.remove(profile);
        *///?} else {
        boolean result = super.remove(profile);
        //?}

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(/*? if <1.21.9 {*//*AutoWhitelist.getServer().getCommandSource()*//*?}*/);
        }

        //? if >=1.21.9 {
        return result;
        //?}
    }

    @Override
    protected ServerConfigEntry</*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*$}*/> fromJson(JsonObject json) {
        ExtendedWhitelistEntry entry = new ExtendedWhitelistEntry(json);

        if (entry.getKey() != null) return entry;
        else return new WhitelistEntry(json);
    }

    public boolean isAllowed(ExtendedPlayerProfile profile) {
        return this.contains(profile);
    }

    protected String toString(ExtendedPlayerProfile gameProfile) {
        return Stonecutter.profileId(gameProfile).toString();
    }

    public Collection<? extends WhitelistEntry> getEntries() {
        return this.values();
    }

    public void remove(String key, Type type) {
        switch (type) {
            case DISCORD_ID -> values().stream()
              .filter(entry -> entry.getKey() instanceof ExtendedPlayerProfile extendedProfile && extendedProfile.getDiscordId().equals(key))
              .forEach(whitelistEntry -> remove(whitelistEntry.getKey()));
            case USERNAME -> values().stream()
              .filter(entry -> entry.getKey() != null)
              .filter(entry -> Stonecutter.profileName(entry.getKey()).equals(key))
              .forEach(whitelistEntry -> remove(whitelistEntry.getKey()));
        }
    }

    public List<ExtendedPlayerProfile> getProfilesFromDiscordId(String id) {
        return values().stream()
          .filter(entry -> entry.getKey() instanceof ExtendedPlayerProfile extendedProfile && extendedProfile.getDiscordId().equals(id))
          .map(whitelistEntry -> (ExtendedPlayerProfile) whitelistEntry.getKey()).toList();
    }

    public List<ExtendedWhitelistEntry> getFromDiscordId(String id) {
        return values().stream()
          .filter(entry -> entry instanceof ExtendedWhitelistEntry)
          .filter(entry -> entry.getKey() instanceof ExtendedPlayerProfile profile && profile.getDiscordId().equals(id))
          .map(v -> (ExtendedWhitelistEntry) v)
          .toList();
    }

    @Nullable
    public /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry getProfileFromUsername(String username) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry -> Stonecutter.profileName(entry.getKey()).equals(username))
          .map(ServerConfigEntry::getKey)
          .findFirst().orElse(null);
    }

    @Nullable
    public /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry getProfileFromUUID(UUID uuid) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry -> Stonecutter.profileId(entry.getKey()).equals(uuid))
          .map(ServerConfigEntry::getKey)
          .findFirst().orElse(null);
    }

    @Nullable
    public WhitelistEntry getEntryFromProfile(/*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry ->
            entry.getKey().equals(profile) ||
            (
              Stonecutter.profileName(entry.getKey()).equals(Stonecutter.profileName(profile)) &&
              Stonecutter.profileId(entry.getKey()).equals(Stonecutter.profileId(entry.getKey()))
            )
          )
          .findFirst().orElse(null);
    }

    public enum Type {
        DISCORD_ID,
        USERNAME
    }
}
