package com.awakenedredstone.autowhitelist.networking;

import com.awakenedredstone.autowhitelist.mixin.authlib.MinecraftClientAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class GeyserAPIClient extends MinecraftClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(GeyserAPIClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeyserAPIClient(String accessToken, Proxy proxy) {
        super(accessToken, proxy);
    }

    public static GeyserAPIClient unauthenticated(final Proxy proxy) {
        return new GeyserAPIClient(null, proxy);
    }

    @Nullable
    @Override
    public <T> T get(@NotNull final URL url, @NotNull final Class<T> responseClass) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(responseClass);
        final HttpURLConnection connection = createUrlConnection(url);
        if (getAccessToken() != null) {
            connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        }

        return readInputStream(url, responseClass, connection);
    }

    @Nullable
    private <T> T readInputStream(final URL url, final Class<T> clazz, final HttpURLConnection connection) {
        InputStream inputStream = null;
        try {
            final int status = connection.getResponseCode();

            final String result;
            if (status < 400) {
                inputStream = connection.getInputStream();
                result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                if (result.isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(result, clazz);
            } else {
                final String contentType = connection.getContentType();
                inputStream = connection.getErrorStream();
                final ErrorResponse errorResponse;
                if (inputStream != null) {
                    result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    if (contentType != null && contentType.startsWith("text/html")) {
                        LOGGER.error("Got an error with a html body connecting to {}: {}", url.toString(), result);
                        throw new MinecraftClientHttpException(status);
                    }
                    errorResponse = objectMapper.readValue(result, ErrorResponse.class);
                    throw new GeyserAPIClientHttpException(status, errorResponse);
                } else {
                    throw new MinecraftClientHttpException(status);
                }
            }
        } catch (final IOException e) {
            //Connection errors
            throw new MinecraftClientException(
              MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE , "Failed to read from " + url + " due to " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    protected HttpURLConnection createUrlConnection(final URL url) {
        return ((MinecraftClientAccessor) this).callCreateUrlConnection(url);
    }

    private String getAccessToken() {
        return ((MinecraftClientAccessor) this).autowhitelist$getAccessToken();
    }

    private Proxy getProxy() {
        return ((MinecraftClientAccessor) this).autowhitelist$getProxy();
    }
}
