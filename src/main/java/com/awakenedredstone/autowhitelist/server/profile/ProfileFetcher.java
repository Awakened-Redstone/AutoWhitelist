package com.awakenedredstone.autowhitelist.server.profile;

import com.awakenedredstone.autowhitelist.LazyConstants;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.util.data.UUIDUtil;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface ProfileFetcher {
    // TODO: error messages
    Optional<PlayerProfile> fetch();

    static @NotNull ProfileFetcher javaFetcher(String input) {
        if (UUIDUtil.isValidUuid(input)) {
            return () -> ServerDetails.getUserCache().get(UUIDUtil.parseUuid(input)).map(PlayerProfile::from);
        }

        return () -> ServerDetails.getUserCache().get(input).map(PlayerProfile::from);
    }

    static @NotNull ProfileFetcher bedrockFetcher(String input) {
        if (UUIDUtil.isValidUuid(input)) {
            return () -> {
                UUID uuid = UUIDUtil.parseUuid(input);
                if (uuid.getMostSignificantBits() != 0) return Optional.empty();
                return Optional.of(new PlayerProfile(uuid, "Bedrock player"));
            };
        }

        if (!StringUtil.isValidPlayerName(input)) {
            return Optional::empty;
        }

        return () -> LazyConstants.GEYSER_PROFILE_REPOSITORY.get()
          .findProfileByName(input)
          .map(profile -> new PlayerProfile(PlayerProfile.id(profile), PlayerProfile.name(profile)));
    }
}
