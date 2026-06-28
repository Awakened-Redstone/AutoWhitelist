package com.awakenedredstone.autowhitelist.network.geyser;

import com.awakenedredstone.autowhitelist.mixin.authlib.MinecraftClientAccessor;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.client.ObjectMapper;
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

    private final ObjectMapper objectMapper = ObjectMapper.create();

    public GeyserAPIClient(Proxy proxy) {
        super(null, proxy);
    }

    public static GeyserAPIClient unauthenticated(final Proxy proxy) {
        return new GeyserAPIClient(proxy);
    }

    @Nullable
    @Override
    public <T> T get(@NotNull final URL url, @NotNull final Class<T> responseClass) throws GeyserAPIException {
        Objects.requireNonNull(url);
        Objects.requireNonNull(responseClass);
        final HttpURLConnection connection = createUrlConnection(url);

        return readInputStream(url, responseClass, connection);
    }

    @Nullable
    private <T> T readInputStream(final URL url, final Class<T> clazz, final HttpURLConnection connection) throws GeyserAPIException {
        InputStream inputStream = null;
        try {
            final int status = connection.getResponseCode();

            final String result;
            if (status < 400) {
                inputStream = connection.getInputStream();
                result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                if (result.isEmpty()) return null;
                return objectMapper.readValue(result, clazz);
            } else {
                final String contentType = connection.getContentType();
                inputStream = connection.getErrorStream();
                final ErrorResponse errorResponse;
                if (inputStream != null) {
                    result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    if (contentType != null && !contentType.startsWith("application/json")) {
                        LOGGER.error("Received non JSON body while connecting to {}: {}", url.toString(), result);
                        throw new GeyserAPIClientHttpException(status);
                    }

                    errorResponse = objectMapper.readValue(result, ErrorResponse.class);
                    throw new GeyserAPIClientHttpException(status, errorResponse);
                } else {
                    throw new GeyserAPIClientHttpException(status);
                }
            }
        } catch (final IOException e) {
            // Connection errors
            throw new GeyserAPIException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE , "Failed to read from " + url + " due to " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    protected HttpURLConnection createUrlConnection(final URL url) {
        return ((MinecraftClientAccessor) this).callCreateUrlConnection(url);
    }
}
