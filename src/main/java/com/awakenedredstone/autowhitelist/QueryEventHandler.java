package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.concurrent.atomic.LateFinal;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.store.DynamicRetriever.Mode;
import com.awakenedredstone.autowhitelist.discord.util.RoleUtils;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.mixin.ServerLoginPacketListenerImplAccessor;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCacheEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteListEntry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class QueryEventHandler implements ServerLoginConnectionEvents.QueryStart {
    public static final Logger LOGGER = LoggerFactory.getLogger(QueryEventHandler.class);
    public static final LateFinal<ComponentContents> NOT_WHITELISTED_MESSAGE = new LateFinal<>();

    @Override
    // TODO: Support configuration phase, use Mixin?
    public void onLoginStart(
      @NotNull ServerLoginPacketListenerImpl handler,
      @NotNull MinecraftServer server,
      @NotNull LoginPacketSender sender,
      @NotNull ServerLoginNetworking.LoginSynchronizer synchronizer
    ) {
        // Don't run the cache if the bot is disabled
        // TODO: support multilink
        if (!DiscordClientHolder.hasTask()) return;

        PlayerList playerList = server.getPlayerList();
        if (!playerList.isUsingWhitelist()) return;

        ServerLoginPacketListenerImplAccessor accessor = (ServerLoginPacketListenerImplAccessor) handler;
        PlayerProfile profile = new PlayerProfile(accessor.getAuthenticatedProfile());

        Optional<Component> denialMessage = Optional.ofNullable(playerList.canPlayerLogin(accessor.getConnection().getRemoteAddress(), profile.asEntryProfile()));
        if (denialMessage.isPresent() && !denialMessage.get().getContents().equals(NOT_WHITELISTED_MESSAGE.get())) return;


        if (denialMessage.isEmpty()) {
            LinkingWhitelist whitelist = WhitelistHandler.getWhitelist(server);
            UserWhiteListEntry whitelistEntry = whitelist.get(profile.asEntryProfile());
            if (whitelistEntry == null) return;
            if (profile.name().equalsIgnoreCase(whitelistEntry.getUser().name())) return;

            // Update the whitelist file to have the new username
            whitelist.add(LinkedWhitelistEntry.withNewName(whitelistEntry, profile.name()));
        } else {
            if (!DiscordClientHolder.isInitialized()) {
                Component message = denialMessage.get();
                if (message instanceof MutableComponent mutable) {
                    handler.disconnect(mutable.append("\n" + Texts.translatedComponent("multiplayer.autowhitelist.disconnect.too_early")));
                }

                return;
            }

            var holder = DiscordClientHolder.getCurrent();
            ScopedValue
              .where(holder.getRetrieverScope(), Mode.FIRST)
              .run(() -> registerToWhitelist(holder, profile, handler));
        }
    }

    private void registerToWhitelist(DiscordClientHolder holder, PlayerProfile profile, ServerLoginPacketListenerImpl handler) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();

        WhitelistCacheEntry cachedEntry = whitelist.getCache().get(profile.asEntryProfile());
        if (cachedEntry == null) return;

        String discordId = cachedEntry.getUser().getDiscordId();
        Member member = holder.getGuild().getMemberById(Snowflake.of(discordId)).onErrorReturn(null).block();
        if (member == null) {
            whitelist.getCache().remove(profile.asEntryProfile());
            return;
        }

        Optional<Role> perhapsRole = RoleUtils.getHighestEntryRole(member);
        if (perhapsRole.isEmpty()) return;

        Role role = perhapsRole.get();

        var extendedProfile = new PlayerProfile(profile.id(), profile.name(), discordId, role.getId().asString(), AutoWhitelist.config().whitelist.lockTime());

        var entry = RoleEntryMap.get(role);
        for (var action : entry.actions()) {
            if (!action.validate()) {
                LOGGER.error("Failed to use whitelist cache due to a broken action {}", entry);
                handler.disconnect(Texts.translatedComponent("multiplayer.autowhitelist.disconnect.internal_error"));
                return;
            }
        }

        WhitelistHandler.whitelistProfile(null, extendedProfile, entry);
    }
}
