package com.awakenedredstone.autowhitelist.server.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.util.RoleUtils;
import com.awakenedredstone.autowhitelist.discord.message.responses.RegisterMessages;
import com.awakenedredstone.autowhitelist.entry.Entry;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.ProfileFetcher;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCache;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCacheEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Optional;

public class WhitelistHandler {
    public static boolean qualifies(Member member) {
        return RoleUtils.getHighestEntryRole(member).isPresent();
    }

    public static boolean qualifies(PlayerProfile profile) {
        if (!profile.isLinked()) return false;
        var member = DiscordClientHolder.getCurrent().getGuild().getMemberById(Snowflake.of(profile.discordId())).onErrorComplete().blockOptional();
        if (member.isEmpty()) {
            return false;
        }
        return RoleUtils.getHighestEntryRole(member.get()).isPresent();
    }

    /**
     * The core registration method. It has been abstracted to allow for easy implementation in many different
     * contexts and scenarios and better maintainability.
     *
     * @param member  The discord member to be whitelisted
     * @param fetcher The profile fetched, an abstraction layer for getting the player UUID and name
     * @param admin   If the registration is run by an admin. This bypasses things such as the lock time.
     * @return A mono with the response details, which are success, the response id and the arguments for the response
     */
    public static Mono<Response> register(Member member, ProfileFetcher fetcher, boolean admin) {
        return register(ServerDetails.getServer(), member, fetcher, admin);
    }

    /**
     * The core registration method. It has been abstracted to allow for easy implementation in many different
     * contexts and scenarios and better maintainability.
     *
     * @param server  The Minecraft server to use
     * @param member  The discord member to be whitelisted
     * @param fetcher The profile fetched, an abstraction layer for getting the player UUID and name
     * @return A mono with the response details, which are success, the response id and the arguments for the response
     */
    public static Mono<Response> register(MinecraftServer server, Member member, ProfileFetcher fetcher, boolean admin) {
        return Mono.fromSupplier(() -> _register(server, member, fetcher, admin));
    }

    private static Response _register(MinecraftServer server, Member member, ProfileFetcher fetcher, boolean admin) {
        Optional<Role> schrodingerRole = RoleUtils.getHighestEntryRole(member);
        if (schrodingerRole.isEmpty()) {
            return new Response(false, RegisterMessages.UNQUALIFIED);
        }

        LinkingWhitelist whitelist = getWhitelist(server);

        Optional<LinkedPlayerProfile> schrodingerCurrentEntry = whitelist.fromDiscordId(member.getId().asString()).map(LinkedWhitelistEntry::getUser);
        if (schrodingerCurrentEntry.isEmpty()) {
            schrodingerCurrentEntry = whitelist.getCache().fromDiscordId(member.getId().asString()).map(WhitelistCacheEntry::getUser);
        } else if (isLocked(schrodingerCurrentEntry.get()) && !admin) {
            return new Response(false, RegisterMessages.LOCKED, schrodingerCurrentEntry.get());
        }

        Optional<PlayerProfile> schrodingerProfile = fetcher.fetch();
        if (schrodingerProfile.isEmpty()) {
            return new Response(false, RegisterMessages.NOT_FOUND);
        }

        PlayerProfile profile = schrodingerProfile.get();
        var playerEntry = profile.asLinkedProfile();

        if (schrodingerCurrentEntry.isPresent() && profile.id().equals(schrodingerCurrentEntry.get().id())) {
            return new Response(false, RegisterMessages.NOTHING_CHANGED);
        }

        UserBanListEntry banEntry = server.getPlayerList().getBans().get(playerEntry);
        if (banEntry != null) {
            if (admin) return new Response(false, RegisterMessages.BANNED, banEntry);
            return new Response(false, RegisterMessages.BANNED);
        }

        if (whitelist.getCache().isCached(playerEntry)) {
            return new Response(false, RegisterMessages.ALREADY_LINKED);
        }

        if (whitelist.isWhiteListed(playerEntry)) {
            return new Response(false, RegisterMessages.ALREADY_WHITELISTED);
        }

        Role role = schrodingerRole.get();
        var entry = RoleEntryMap.get(role);

        for (EntryAction<?> action : entry.actions()) {
            if (!action.validate()) {
                return new Response(false, RegisterMessages.BROKEN_ACTION, role, action);
            }
        }

        PlayerProfile linkedProfile = new PlayerProfile(profile.id(), profile.name(), member.getId().asString(), role.getId().asString(), AutoWhitelist.config().whitelist.lockTime());

        if (!whitelistProfile(schrodingerCurrentEntry.orElse(null), linkedProfile, entry)) {
            return new Response(false, RegisterMessages.ERROR_WHILE_ADDING, linkedProfile);
        }

        return new Response(true, RegisterMessages.REGISTERED, linkedProfile);
    }

    public static boolean whitelistProfile(@Nullable /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId oldProfile, @NotNull PlayerProfile profile, @NotNull Entry entry) {
        LinkingWhitelist whitelist = getWhitelist();

        if (!whitelist.update(oldProfile, new LinkedWhitelistEntry(profile.asLinkedProfile()), entry)) {
            AutoWhitelist.LOGGER.error("Failed to update user entry, check above for the error");
            return false;
        }

        return true;
    }

    public static void unlink(Member member) {
        var schrodingerEntry = getWhitelist().getCache().fromDiscordId(member.getId().asString());
        if (schrodingerEntry.isEmpty()) return;

        schrodingerEntry.ifPresent(entry -> {
            getWhitelist().remove(entry.getUser());
            getWhitelist().getCache().remove(entry.getUser());
        });
    }

    public static void remove(Member member) {
        remove(member.getId().asString());
    }

    public static void remove(String id) {
        Optional<LinkedWhitelistEntry> schrodingerEntry = getWhitelist().fromDiscordId(id);
        if (schrodingerEntry.isEmpty()) return;

        schrodingerEntry.ifPresent(entry -> getWhitelist().remove(entry));
    }

    public static void revalidateEntries() {
        if (!DiscordClientHolder.hasGuild()) return;
        LinkingWhitelist whitelist = getWhitelist();

        whitelistEntries:
        for (UserWhiteListEntry entry : new ArrayList<>(whitelist.getEntries())) {
            PlayerProfile profile = PlayerProfile.from(entry.getUser());
            if (!profile.isLinked()) continue;

            Optional<Member> schrodingerMember = DiscordClientHolder.getCurrent().getGuild().getMemberById(Snowflake.of(profile.discordId())).onErrorComplete().blockOptional();
            Optional<Role> schrodingerRole;
            if (schrodingerMember.isEmpty() || (schrodingerRole = RoleUtils.getHighestEntryRole(schrodingerMember.get())).isEmpty()) {
                AutoWhitelist.LOGGER.warn("Removing user %s (id %s) no longer matching criteria".formatted(profile.name(), profile.discordId()));
                whitelist.remove(entry);
                continue;
            }

            Role role = schrodingerRole.get();
            if (profile.role().equals(role.getId().asString())) continue;

            var allowEntry = RoleEntryMap.get(role);
            for (EntryAction<?> action : allowEntry.actions()) {
                if (!action.validate()) continue whitelistEntries;
            }

            PlayerProfile linkedProfile = new PlayerProfile(profile.asLinkedProfile().withRole(role));
            whitelistProfile(entry.getUser(), linkedProfile, allowEntry);
        }
    }

    @NotNull
    public static LinkingWhitelist getWhitelist() {
        return getWhitelist(ServerDetails.getServer());
    }

    @NotNull
    public static LinkingWhitelist getWhitelist(MinecraftServer server) {
        return (LinkingWhitelist) server.getPlayerList().getWhiteList();
    }

    @NotNull
    public static WhitelistCache getCache() {
        return getCache(ServerDetails.getServer());
    }

    @NotNull
    public static WhitelistCache getCache(MinecraftServer server) {
        return getWhitelist(server).getCache();
    }

    public static boolean isLocked(/*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        return isLocked(ServerDetails.getServer(), profile);
    }

    public static boolean isLocked(MinecraftServer server, /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        long lockedUntil = profile instanceof LinkedPlayerProfile linkedProfile ? linkedProfile.getLockedUntil() : -1;
        return lockedUntil > System.currentTimeMillis() || AutoWhitelist.config().whitelist.lockTime() == -1 || lockedUntil == -1 || server.getPlayerList().getBans().isBanned(profile);
    }

    public record Response(boolean success, Identifier replyId, Object... args) {
    }
}
