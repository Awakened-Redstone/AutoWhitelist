package com.awakenedredstone.autowhitelist.server.whitelist.link;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.Entry;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCache;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCacheEntry;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
/*? if >=1.21.9 {*/ import net.minecraft.server.notifications.NotificationService; /*?}*/
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class LinkingWhitelist extends UserWhiteList {
    public static final File CACHE_FILE = new File("whitelist-cache.json");
    private final WhitelistCache cache;

    public LinkingWhitelist(File file/*? if >=1.21.9 {*/, NotificationService managementListener /*?}*/) {
        super(file/*? if >=1.21.9 {*/, managementListener /*?}*/);
        this.cache = new WhitelistCache(CACHE_FILE/*? if >=1.21.9 {*/, managementListener /*?}*/);
    }

    @Override
    public /*$ entryPatchReturn >>*/boolean remove(@NotNull /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId key) {
        PlayerProfile profile = PlayerProfile.from(key);
        if (!remove(profile)) return /*? if >=1.21.9 {*/false/*?}*/;

        //? if <1.21.9 {
        /*super.remove(profile);
        *///?} else {
        boolean result = super.remove(profile.asLinkedProfile());
        //?}

        if (ServerDetails.getServer().getPlayerList().isUsingWhitelist()) {
            ServerDetails.getServer().kickUnlistedPlayers(/*? if <1.21.9 {*//*AutoWhitelist.getServer().getCommandSource()*//*?}*/);
        }

        //? if >=1.21.9 {
        return result;
        //?}
    }

    @Override
    public /*$ entryPatchReturn >>*/boolean add(@NotNull UserWhiteListEntry whitelistEntry) {
        if (whitelistEntry instanceof LinkedWhitelistEntry linkedEntry) {
            cache.add(new WhitelistCacheEntry(linkedEntry.getUser()));
        }

        return super.add(whitelistEntry);
    }

    public /*$ entryPatchReturn >>*/boolean update(@Nullable /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId oldProfile, UserWhiteListEntry whiteListEntry, Entry entry) {
        if (oldProfile != null) {
            if (!remove(PlayerProfile.from(oldProfile))) return /*? if >=1.21.9 {*/false/*?}*/;
            cache.remove(oldProfile);
        }

        PlayerProfile profile = PlayerProfile.from(whiteListEntry.getUser());
        for (EntryAction<?> action : entry.actions()) {
            AutoWhitelist.LOGGER.debug("Applying addition action [type: {}, target: {}, role: {}]", action.getType().id(), profile.name(), profile.role());
            action.onAdd(profile);
        }

        /*? if >=1.21.9 {*/return/*?}*/ this.add(whiteListEntry);
    }

    @Override
    protected @NotNull StoredUserEntry</*$ WhitelistProfile {*/net.minecraft.server.players.NameAndId/*$}*/> createEntry(@NotNull JsonObject json) {
        LinkedWhitelistEntry entry = new LinkedWhitelistEntry(json);

        if (entry.getUser() != null) return entry;
        else return new UserWhiteListEntry(json);
    }

    @Override
    public void load() throws IOException {
        super.load();
        this.cache.load();
    }

    @Override
    public void save() throws IOException {
        super.save();
        this.cache.save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean remove(@NotNull PlayerProfile profile) {
        if (profile.isLinked()) {
            var entry = RoleEntryMap.getNullable(profile.role());
            if (entry != null) {
                for (var action : entry.actions()) {
                    if (!action.validate()) {
                        AutoWhitelist.LOGGER.error("Failed to remove {} from the whitelist due to the action {} not being valid", profile.name(), entry);
                        // Broadcast to all operators that there was an error
                        ServerDetails.getServer().createCommandSourceStack().sendSuccess(() -> Component.literal("Failed to remove player from whitelist. Check the server logs for more details."), true);
                        return false;
                    }
                }

                for (var action : entry.actions()) {
                    AutoWhitelist.LOGGER.debug("Applying removal action [type: {}, target: {}, role: {}]", action.getType().id(), profile.name(), profile.role());
                    action.onRemove(profile);
                }
            }
        }

        return true;
    }

    public UserWhiteListEntry getOrCreateEntry(/*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        if (this.contains(profile)) {
            return get(profile);
        }

        return new UserWhiteListEntry(profile);
    }

    public Optional<LinkedWhitelistEntry> fromDiscordId(String id) {
        for (UserWhiteListEntry entry : getEntries()) {
            if (entry instanceof LinkedWhitelistEntry linkedEntry && id.equals(linkedEntry.getUser().getDiscordId())) {
                return Optional.of(linkedEntry);
            }
        }

        return Optional.empty();
    }

    public Optional<UserWhiteListEntry> fromName(String name) {
        for (var entry : this.getEntries()) {
            if (entry.getUser().name().equals(name)) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    public WhitelistCache getCache() {
        return cache;
    }
}
