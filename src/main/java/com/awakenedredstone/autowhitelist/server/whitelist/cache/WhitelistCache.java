package com.awakenedredstone.autowhitelist.server.whitelist.cache;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.server.profile.LinkedNameAndId;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
/*? if >=1.21.9 {*/ import discord4j.common.store.action.gateway.GatewayActions;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.gateway.GuildMemberRemove;
import net.minecraft.server.notifications.NotificationService; /*?}*/
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public class WhitelistCache extends StoredUserList<LinkedNameAndId, @NotNull WhitelistCacheEntry> {
    public WhitelistCache(File file/*? if >=1.21.9 {*/, NotificationService managementListener /*?}*/) {
        super(file/*? if >=1.21.9 {*/, managementListener /*?}*/);
    }

    @Override
    protected StoredUserEntry<LinkedNameAndId> createEntry(JsonObject json) {
        return new WhitelistCacheEntry(json);
    }

    public boolean isCached(final LinkedNameAndId profile) {
        return this.contains(profile);
    }

    @Override
    protected @NotNull String getKeyForUser(@NotNull LinkedNameAndId gameProfile) {
        return PlayerProfile.id(gameProfile).toString();
    }

    @Override
    public /*$ entryPatchReturn >>*/boolean add(WhitelistCacheEntry entry) {
        assert entry.getUser() != null;
        if (DiscordClientHolder.hasGuild()) {
            DiscordClientHolder discord = DiscordClientHolder.getCurrent();
            discord.getGuild().requestMembers(Set.of(Snowflake.of(entry.getUser().getDiscordId()))).subscribe();
        }
        //? if <1.21.9 {
        /*super.add(entry);
        *///?} else {
        return super.add(entry);
        //?}
    }

    @Override
    public boolean remove(LinkedNameAndId user) {
        if (DiscordClientHolder.isInitialized()) {
            DiscordClientHolder discord = DiscordClientHolder.getCurrent();
            GatewayDiscordClient client = discord.getClient();
            discord.getGuild().getMemberById(Snowflake.of(user.getDiscordId()))
              .flatMapMany(member -> {
                  var memberRemove = GuildMemberRemove.builder()
                    .guildId(member.getGuildId().asLong())
                    .user(member.getUserData())
                    .build();
                  return client.getGatewayResources().getStore().execute(GatewayActions.guildMemberRemove(0, memberRemove));
              }).subscribe();
        }

        return super.remove(user);
    }

    public Optional<WhitelistCacheEntry> fromName(String name) {
        for (var entry : this.getEntries()) {
            if (entry.getUser().name().equals(name)) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    public Optional<WhitelistCacheEntry> fromDiscordId(String id) {
        for (var entry : this.getEntries()) {
            if (entry.getUser().getDiscordId().equals(id)) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    @Nullable
    public WhitelistCacheEntry get(GameProfile key) {
        return super.get(new LinkedNameAndId(key));
    }

    public void remove(GameProfile key) {
        super.remove(new LinkedNameAndId(key));
    }

    //? if >=1.21.9 {
    @Nullable
    public WhitelistCacheEntry get(net.minecraft.server.players.NameAndId key) {
        return super.get(new LinkedNameAndId(key));
    }

    public void remove(net.minecraft.server.players.NameAndId key) {
        super.remove(new LinkedNameAndId(key));
    }
    //?}
}
