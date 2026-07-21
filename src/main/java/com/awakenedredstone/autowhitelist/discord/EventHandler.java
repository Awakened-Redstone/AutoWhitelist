package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.discord.util.Reactor;
import com.awakenedredstone.autowhitelist.discord.util.RoleUtils;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ReconnectEvent;
import discord4j.core.object.entity.Role;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventHandler {
    public static <E extends Event, T> void listen(GatewayDiscordClient client, Class<E> eventClass, Function<E, Publisher<T>> mapper) {
        client.on(eventClass, event ->
          Reactor.elastic(() -> mapper.apply(event))
            /*.doOnError(throwable -> DiscordClientHolder.LOGGER.error(
              format(Context.of(LogUtil.KEY_SHARD_ID, event.getShardInfo().getIndex()), "Error while handling {}"),
              event.getClass().getSimpleName(),
              throwable
            ))*/
        ).subscribe();
    }

    public static <E extends Event> void listen(GatewayDiscordClient client, Class<E> eventClass, Consumer<E> mapper) {
        listen(client, eventClass, e -> {
            mapper.accept(e);
            return Mono.empty();
        });
    }

    public static void handleEvents(GatewayDiscordClient client) {
        // Lifecycle events
//        listen(client, ReadyEvent.class, EventHandler::handleConnect);
        listen(client, ReconnectEvent.class, EventHandler::handleReconnect);

        // Gateway events
        listen(client, MemberUpdateEvent.class, EventHandler::handleMemberUpdate);
        listen(client, MemberLeaveEvent.class, EventHandler::handleMemberLeave);
    }

    private static void handleMemberUpdate(MemberUpdateEvent event) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        Optional<LinkedWhitelistEntry> schrodingerEntry = whitelist.fromDiscordId(event.getMemberId().asString());
        if (schrodingerEntry.isEmpty()) {
            return;
        }

        var highestRole = RoleUtils.collectRoles(event.getCurrentRoles()).blockOptional().orElseThrow().stream().filter(RoleEntryMap::containsRole).findFirst();

        // Nothing to do, the role didn't change
        if (Optional.ofNullable(schrodingerEntry.get().getUser().getRole()).equals(highestRole.map(Role::getId).map(Snowflake::asString))) {
            return;
        }

        // User no longer qualifies and should be removed
        if (highestRole.isEmpty()) {
            WhitelistHandler.remove(event.getMemberId().asString());
            return;
        }

        LinkedWhitelistEntry entry = schrodingerEntry.get();
        WhitelistHandler.whitelistProfile(entry.getUser(), new PlayerProfile(entry.getUser().withRole(highestRole.get())), RoleEntryMap.get(highestRole.get()));
    }

    private static void handleMemberLeave(MemberLeaveEvent event) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        Optional<LinkedWhitelistEntry> schrodingerEntry = whitelist.fromDiscordId(event.getUser().getId().asString());
        schrodingerEntry.ifPresent(entry -> WhitelistHandler.getWhitelist().remove(entry));
    }

    private static void handleReconnect(ReconnectEvent event) {
        WhitelistHandler.revalidateEntries();
        if (DiscordClientHolder.hasGuild()) {
            var entries = WhitelistHandler.getCache().getEntries().stream().map(entry -> Snowflake.of(entry.getUser().getDiscordId())).collect(Collectors.toSet());
            DiscordClientHolder.getCurrent().getGuild().requestMembers(entries).subscribe();
        }
    }
}
