package com.awakenedredstone.autowhitelist.networking;

import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

public class GeyserProfileRepository implements GameProfileRepository {
    public static final Logger LOGGER = LoggerFactory.getLogger(GeyserProfileRepository.class);
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_USERS = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final GeyserAPIClient client;
    private final HttpUrl searchPageUrl;

    public GeyserProfileRepository() {
        this.client = GeyserAPIClient.unauthenticated(Proxy.NO_PROXY);
        searchPageUrl = new HttpUrl.Builder().scheme("https").host("api.geysermc.org").addPathSegments("/v2/xbox/xuid/").build();
    }

    @Override
    public void findProfilesByNames(String[] names /*? if <1.20.2 {*//*, final Agent agent*//*?}*/, ProfileLookupCallback callback) {
        final Set<String> criteria = Arrays.stream(names)
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toSet());

        for (final String request : criteria) {
            final String normalizedRequest = normalizeName(request);

            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    final ProfileSearchResultsResponse response = client.get(searchPageUrl.newBuilder().addPathSegment(normalizedRequest).build().url(), ProfileSearchResultsResponse.class);
                    failCount = 0;

                    if (response == null) {
                        LOGGER.debug("Couldn't find profile {}", normalizedRequest);
                        callback.onProfileLookupFailed(error(normalizedRequest), new ProfileNotFoundException("Server did not find the requested profile"));
                        sleep(DELAY_BETWEEN_USERS);
                        continue;
                    }

                    LOGGER.debug("Successfully looked up profile {}", normalizedRequest);
                    callback.onProfileLookupSucceeded(new GameProfile(new UUID(0, response.xuid()), normalizedRequest));

                    sleep(DELAY_BETWEEN_USERS);
                } catch (final MinecraftClientException e) {
                    if (e instanceof GeyserAPIClientHttpException httpException) {
                        if (httpException.hasError("Unable to find user in our cache. Please try specifying their Floodgate UUID instead")) {
                            LOGGER.debug("Couldn't find profile {} [User not in cache]", normalizedRequest);
                            callback.onProfileLookupFailed(error(normalizedRequest), new ProfileNotFoundException("Server did not find the requested profile in the cache"));
                            sleep(DELAY_BETWEEN_USERS);
                            continue;
                        }
                    }
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        LOGGER.debug("Couldn't find profile {} because of a server error", normalizedRequest);
                        callback.onProfileLookupFailed(error(normalizedRequest), e.toAuthenticationException());
                    } else {
                        sleep(DELAY_BETWEEN_FAILURES);
                        failed = true;
                    }
                }
            } while (failed);
        }
    }

    public Optional<GameProfile> findProfileByName(String name) {
        final String normalizedRequest = normalizeName(name);

        try {
            final ProfileSearchResultsResponse response = client.get(searchPageUrl.newBuilder().addPathSegment(normalizedRequest).build().url(), ProfileSearchResultsResponse.class);

            if (response != null) {
                return Optional.of(new GameProfile(new UUID(0, response.xuid()), normalizedRequest));
            }
        } catch (final MinecraftClientException e) {
            LOGGER.warn("Couldn't find profile with name: {}", name, e);
        }

        return Optional.empty();
    }

    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static /*? if <1.20.2 {*//*GameProfile*//*?} else {*/String/*?}*/ error(String request) {
        return /*? if <1.20.2 {*//*new GameProfile(null, request)*//*?} else {*/request/*?}*/;
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException ignored) {
        }
    }

    public record ProfileSearchResultsResponse(long xuid) {}
}
