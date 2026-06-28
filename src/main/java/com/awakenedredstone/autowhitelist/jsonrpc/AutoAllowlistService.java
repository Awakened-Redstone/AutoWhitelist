package com.awakenedredstone.autowhitelist.jsonrpc;

import com.awakenedredstone.autowhitelist.jsonrpc.internalapi.LinkedAllowListService;
import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerDto;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AutoAllowlistService {
    public static List<LinkedPlayerDto> get(MinecraftApi api) {
        return api.allowListService()
          .getEntries()
          .stream()
          .filter(userWhiteListEntry -> userWhiteListEntry.getUser() != null)
          .map(userWhiteListEntry -> LinkedPlayerDto.from(userWhiteListEntry.getUser()))
          .toList();
    }

    public static List<LinkedPlayerDto> add(MinecraftApi api, List<LinkedPlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<NameAndId>>> list = players.stream()
          .map(playerDto -> api.playerListService().getUser(playerDto.id(), playerDto.name()))
          .toList();

        for (Optional<NameAndId> optional : Util.sequence(list).join()) {
            optional.ifPresent(nameAndId -> api.allowListService().add(new UserWhiteListEntry(nameAndId), clientInfo));
        }

        return get(api);
    }

    public static List<LinkedPlayerDto> register(MinecraftApi api, List<LinkedPlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<PlayerProfile>>> list = players.stream()
          .map(dto -> api.playerListService().getUser(dto.id(), dto.name())
            .thenApply(optional -> {
                if (optional.isEmpty()) return Optional.<PlayerProfile>empty();

                NameAndId nameAndId = optional.get();
                return Optional.of(new PlayerProfile(nameAndId.id(), nameAndId.name(), dto.role().orElse(null), dto.discordId().orElse(null), dto.lockedUntil().orElse(-1L)));
            }))
          .toList();

        for (Optional<PlayerProfile> optional : Util.sequence(list).join()) {
            optional.ifPresent(profile -> ((LinkedAllowListService) api.allowListService()).register(profile, clientInfo));
        }

        return get(api);
    }
}
