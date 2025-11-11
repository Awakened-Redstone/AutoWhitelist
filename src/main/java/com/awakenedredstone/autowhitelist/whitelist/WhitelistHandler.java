package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.stonecutter.PlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import discord4j.core.object.entity.Member;
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

        if (whitelist.isAllowed(profile.toEntryType()) && whitelist.get(profile.toEntryType()).isLocked()) {

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
