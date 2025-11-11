package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.stonecutter.PlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import discord4j.core.object.entity.Member;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class WhitelistHandler {
    public static boolean registerUser(Member user, ProfileFetcher fetcher, RegistrationCallback callback) {
        Optional<BaseEntryAction> schrodingerAction = RoleActionMap.get(user);
        if (schrodingerAction.isEmpty()) {
            callback.handle(AutoWhitelist.id("error/unqualified"));
            return false;
        }

        Optional<PlayerProfile> schrodingerProfile = fetcher.fetch();
        if (schrodingerProfile.isEmpty()) {
            callback.handle(AutoWhitelist.id("error/not_found"));
            return false;
        }

        PlayerProfile profile = schrodingerProfile.get();
        LinkingWhitelist whitelist = (LinkingWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
        /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry playerEntry = profile.toEntryType();



        if (whitelist.isLocked(playerEntry)) {
            WhitelistEntry whitelistEntry = whitelist.get(playerEntry);
            long lockTime = whitelistEntry instanceof LinkedWhitelistEntry linkedEntry ? linkedEntry.getProfile().getLockedUntil() : -1;

            callback.handle(AutoWhitelist.id("error/locked"), lockTime);
            return false;
        }


    }

    public static LinkingWhitelist getWhitelist() {
        return (LinkingWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
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
