package com.awakenedredstone.autowhitelist.network.geyser;

import com.awakenedredstone.autowhitelist.Constants;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.MinecraftClientException;
/*? if >=1.21.9 {*/ import com.mojang.authlib.yggdrasil.response.NameAndId; /*?}*/
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

public class GeyserProfileRepository implements GameProfileRepository {
    public static final Logger LOGGER = LoggerFactory.getLogger(GeyserProfileRepository.class);

    private final GeyserAPIClient client = GeyserAPIClient.unauthenticated(Proxy.NO_PROXY);
    private final String geyserXuidApi = Constants.GEYSER_API + "/xbox/xuid/";
    // private final String fallbackApi = Constants.FALLBACK_API + "/lookup/bedrock/";

    @Override
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
        final Set<String> nameSet = Arrays.stream(names)
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toSet());

        for (final String name : nameSet) {
            try {
                long xuid = findXuidByGamertag(name);
                callback.onProfileLookupSucceeded(name, new UUID(0, xuid));
            } catch (GeyserAPIException e) {
                callback.onProfileLookupFailed(normalizeName(name), e);
            }
        }
    }

    public Optional</*? if <1.21.9 {*//*GameProfile*//*?} else {*/NameAndId/*?}*/> findProfileByName(String name) {
        try {
            long xuid = findXuidByGamertag(name);

            return Optional.of(new /*? if <1.21.9 {*//*GameProfile*//*?} else {*/NameAndId/*?}*/(new UUID(0, xuid), name));
        } catch (GeyserAPIClientHttpException e) {
            if (e.getStatus() != 503) return Optional.empty();

            LOGGER.warn("Couldn't find profile with name: {}", name, e);
        } catch (MinecraftClientException e) {
            LOGGER.warn("Couldn't find profile with name: {}", name, e);
        }

        return Optional.empty();
    }

    public long findXuidByGamertag(String name) throws GeyserAPIException {
        final GeyserApiXuidResponse response = client.get(HttpAuthenticationService.constantURL(geyserXuidApi + normalizeName(name)), GeyserApiXuidResponse.class);

        if (response != null) {
            return response.xuid;
        }

        throw new GeyserAPIException(MinecraftClientException.ErrorType.JSON_ERROR, "Received empty response body");
    }

    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public record GeyserApiXuidResponse(long xuid) {
    }

    public record MCProfileBedrockApiResponse(String xuid) {
    }
}
