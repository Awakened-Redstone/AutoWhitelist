package com.awakenedredstone.autowhitelist.networking;

import com.mojang.authlib.exceptions.MinecraftClientException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.StringJoiner;

public class GeyserAPIClientHttpException extends MinecraftClientException {
    private final int status;
    private final @Nullable ErrorResponse response;

    public GeyserAPIClientHttpException(final int status) {
        super(ErrorType.HTTP_ERROR, getErrorMessage(status, null));
        this.status = status;
        this.response = null;
    }

    public GeyserAPIClientHttpException(final int status, final @Nullable ErrorResponse response) {
        super(ErrorType.HTTP_ERROR, getErrorMessage(status, response));
        this.status = status;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public Optional<ErrorResponse> getResponse() {
        return Optional.ofNullable(response);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GeyserAPIClientHttpException.class.getSimpleName() + "[", "]")
          .add("type=" + type)
          .add("status=" + status)
          .add("response=" + response)
          .toString();
    }

    public Optional<String> getError() {
        return getResponse()
          .map(ErrorResponse::message)
          .filter(StringUtils::isNotEmpty);
    }

    private static String getErrorMessage(final int status, final ErrorResponse response) {
        final String errorMessage;
        if (response != null) {
            if (StringUtils.isNotEmpty(response.message())) {
                errorMessage = response.message();
            } else {
                errorMessage = "Status: " + status;
            }
        } else {
            errorMessage = "Status: " + status;
        }
        return errorMessage;
    }

    public boolean hasError(final String error) {
        return getError()
          .filter(value -> value.equalsIgnoreCase(error))
          .isPresent();
    }
}