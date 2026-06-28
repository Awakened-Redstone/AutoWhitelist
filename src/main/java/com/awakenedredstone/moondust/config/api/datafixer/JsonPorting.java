package com.awakenedredstone.moondust.config.api.datafixer;

import com.awakenedredstone.moondust.jankson.element.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record JsonPorting(@NotNull JsonObject source, @NotNull JsonObject target) {
    public void copy(String key) {
        target.put(key, Objects.requireNonNull(source.get(key)));
    }

    public void migrate(String newKey, String oldKey) {
        target.put(newKey, Objects.requireNonNull(source.get(oldKey)));
    }

    public JsonPorting getTarget(String key) {
        return new JsonPorting(source, Objects.requireNonNull(target.getObject(key)));
    }
}
