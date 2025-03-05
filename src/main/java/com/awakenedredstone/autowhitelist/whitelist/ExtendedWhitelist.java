package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ExtendedWhitelist extends Whitelist {
    private boolean dirty = false;

    public ExtendedWhitelist(File file) {
        super(file);
    }

    public WhitelistEntry getEntry(GameProfile profile) {
        if (this.isAllowed(profile)) {
            return getEntryFromProfile(profile);
        }

        return new WhitelistEntry(profile);
    }

    @Override
    public void remove(GameProfile key) {
        GameProfile profile;
        if (key instanceof ExtendedGameProfile extendedGameProfile) {
            profile = extendedGameProfile;
        } else if (key.getId().getMostSignificantBits() == 0) {
            GameProfile storedProfile = getProfileFromUUID(key.getId());
            if (storedProfile instanceof ExtendedGameProfile extendedGameProfile) {
                profile = extendedGameProfile;
            } else {
                profile = key;
            }
        } else {
            profile = key;
        }

        if (profile instanceof ExtendedGameProfile extendedProfile) {
            BaseEntryAction entryAction = RoleActionMap.getNullable(extendedProfile.getRole());
            if (entryAction != null) {
                if (entryAction.isValid()) {
                    entryAction.removeUser(extendedProfile);
                } else {
                    AutoWhitelist.LOGGER.error("Failed to remove {} from the whitelist due to the entry {} not being valid", profile.getName(), entryAction);
                    AutoWhitelist.getCommandSource().sendFeedback(() -> Text.literal("Failed to remove player from whitelist. Check the server logs for more details."), true);
                    return;
                }
            }
        }

        super.remove(profile);

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(AutoWhitelist.getServer().getCommandSource());
        }
    }

    protected ServerConfigEntry<GameProfile> fromJson(JsonObject json) {
        ExtendedWhitelistEntry entry = new ExtendedWhitelistEntry(json, this);
        try {
            if (entry.getKey() != null) return entry;
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

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void remove(String key, Type type) {
        switch (type) {
            case DISCORD_ID -> values().stream()
              .filter(entry -> entry.getKey() instanceof ExtendedGameProfile extendedProfile && extendedProfile.getDiscordId().equals(key))
              .forEach(whitelistEntry -> remove(whitelistEntry.getKey()));
            case USERNAME -> values().stream()
              .filter(entry -> entry.getKey() != null)
              .filter(entry -> entry.getKey().getName().equals(key))
              .forEach(whitelistEntry -> remove(whitelistEntry.getKey()));
        }
    }

    public List<ExtendedGameProfile> getProfilesFromDiscordId(String id) {
        return values().stream()
          .filter(entry -> entry.getKey() instanceof ExtendedGameProfile extendedProfile && extendedProfile.getDiscordId().equals(id))
          .map(whitelistEntry -> (ExtendedGameProfile) whitelistEntry.getKey()).toList();
    }

    public List<ExtendedWhitelistEntry> getFromDiscordId(String id) {
        return values().stream()
          .filter(entry -> entry instanceof ExtendedWhitelistEntry)
          .filter(entry -> entry.getKey() instanceof ExtendedGameProfile profile && profile.getDiscordId().equals(id))
          .map(v -> (ExtendedWhitelistEntry) v)
          .toList();
    }

    @Nullable
    public GameProfile getProfileFromUsername(String username) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry -> entry.getKey().getName().equals(username))
          .map(ServerConfigEntry::getKey)
          .findFirst().orElse(null);
    }

    @Nullable
    public GameProfile getProfileFromUUID(UUID uuid) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry -> entry.getKey().getId().equals(uuid))
          .map(ServerConfigEntry::getKey)
          .findFirst().orElse(null);
    }

    @Nullable
    public WhitelistEntry getEntryFromProfile(GameProfile profile) {
        return values().stream()
          .filter(entry -> entry.getKey() != null)
          .filter(entry ->
            entry.getKey().equals(profile) ||
            (
              entry.getKey().getName().equals(profile.getName()) &&
              entry.getKey().getId().equals(profile.getId())
            )
          )
          .findFirst().orElse(null);
    }

    @Override
    public void load() throws IOException {
        super.load();
        if (this.dirty) {
            this.dirty = false;
            this.save();
        }
    }

    public enum Type {
        DISCORD_ID,
        USERNAME
    }
}
