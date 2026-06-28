package com.awakenedredstone.moondust.jankson.element;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A class for internal use in JSON elements that have inner elements, such as arrays and objects
 */
@NullMarked
public final class InnerEntry<T> {
    private final T key;
    private JsonElement value;
    private Meta meta;

    public InnerEntry(T key, @Nullable JsonElement value, @Nullable Meta meta) {
        this.key = key;
        this.value = value != null ? value : JsonNull.INSTANCE;
        this.meta = meta != null ? meta : new Meta();
    }

    public InnerEntry(T key, @Nullable JsonElement value) {
        this.key = key;
        this.value = value != null ? value : JsonNull.INSTANCE;
        this.meta = new Meta();
    }

    public void setValue(@Nullable JsonElement value) {
        this.value = value != null ? value : JsonNull.INSTANCE;
    }

    public void setMeta(@Nullable Meta meta) {
        this.meta = meta != null ? meta : new Meta();
    }

    public T getKey() {
        return key;
    }

    public JsonElement getValue() {
        return value;
    }

    public Meta getMeta() {
        return meta;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof InnerEntry<?> entry)) return false;
        if (!Objects.equals(meta, entry.meta)) return false;
        if (!key.equals(entry.key)) return false;

        return value.equals(entry.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, meta);
    }

    public static final class Meta {
        private @Nullable String comment;
        private @Nullable String secret;

        public Meta() {}


        public Meta(@Nullable String comment, @Nullable String secret) {
            this.comment = comment;
            this.secret = secret;
        }

        @Nullable
        public String getComment() {
            return comment;
        }

        public void setComment(@Nullable String comment) {
            this.comment = comment;
        }

        public boolean isSecret() {
            return secret != null;
        }

        @Nullable
        public String secretPlaceholder() {
            return secret;
        }

        public void setSecret(@Nullable String secret) {
            this.secret = secret;
        }
    }
}
