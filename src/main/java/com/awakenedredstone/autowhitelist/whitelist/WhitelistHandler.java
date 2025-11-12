package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.Constants;
import com.awakenedredstone.autowhitelist.discord.util.RoleUtils;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.stonecutter.PlayerProfile;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WhitelistHandler {
    public static boolean register(Member user, ProfileFetcher fetcher, RegistrationCallback callback) {
        Optional<Role> schrodingerRole = RoleUtils.getHighestEntryRole(user);
        if (schrodingerRole.isEmpty()) {
            callback.handle(AutoWhitelist.id("fail/unqualified"));
            return false;
        }

        LinkingWhitelist whitelist = getWhitelist();

        @Nullable
        WhitelistEntry currentEntry = whitelist.get(new LinkedPlayerProfile(Constants.UUID_ZERO, "", null, user.getId().asString(), -1));
        if (currentEntry != null && isLocked(currentEntry.getKey())) {
            callback.handle(AutoWhitelist.id("fail/locked"), currentEntry);
            return false;
        }

        Optional<PlayerProfile> schrodingerProfile = fetcher.fetch();
        if (schrodingerProfile.isEmpty()) {
            callback.handle(AutoWhitelist.id("fail/not_found"));
            return false;
        }

        PlayerProfile profile = schrodingerProfile.get();
        /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry playerEntry = profile.toEntryType();

        if (currentEntry != null && playerEntry.equals(currentEntry.getKey())) {
            callback.handle(AutoWhitelist.id("fail/nothing_changed"));
            return false;
        }

        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(playerEntry)) {
            callback.handle(AutoWhitelist.id("fail/banned"));
            return false;
        }

        if (whitelist.isAllowed(playerEntry)) {
            callback.handle(AutoWhitelist.id("fail/whitelisted"));
            return false;
        }

        Role role = schrodingerRole.get();
        BaseEntryAction action = RoleActionMap.get(role);
        if (action.isValid()) {
            callback.handle(AutoWhitelist.id("fail/broken_action"), action);
        }

        var linkedProfile = new LinkedPlayerProfile(profile.id(), profile.name(), role.getId().asString(), user.getId().asString(), AutoWhitelist.CONFIG.lockTime());

        whitelistProfile(currentEntry, linkedProfile, action);

        callback.handle(AutoWhitelist.id("registered"));
        return true;
    }

    public static void whitelistProfile(@Nullable WhitelistEntry currentEntry, @NotNull LinkedPlayerProfile linkedProfile, @NotNull BaseEntryAction action) {
        LinkingWhitelist whitelist = getWhitelist();
        if (currentEntry != null) {
            whitelist.remove(currentEntry.getKey());
        }

        whitelist.add(new LinkedWhitelistEntry(linkedProfile));

        AutoWhitelist.LOGGER.debug("Whitelisting {} for action of type {} (Role: {})", Stonecutter.profileName(linkedProfile), action.getType(), linkedProfile.getRole());
        action.registerUser(linkedProfile);
    }

    @NotNull
    public static LinkingWhitelist getWhitelist() {
        return (LinkingWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
    }

    public static boolean isLocked(/*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile) {
        long lockedUntil = profile instanceof LinkedPlayerProfile linkedProfile ? linkedProfile.getLockedUntil() : -1;
        return lockedUntil > System.currentTimeMillis() || AutoWhitelist.CONFIG.lockTime() == -1 || lockedUntil == -1 || AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(profile);
    }

    @FunctionalInterface
    public interface RegistrationCallback {
        void handle(Identifier id, Object... args);
    }

    @FunctionalInterface
    public interface ProfileFetcher {
        Optional<PlayerProfile> fetch();
    }
}
