package com.awakenedredstone.autowhitelist.util;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<>();

    public static <K, V> Map<K, V> single(K key, V value) {
        return new MapBuilder<K, V>().put(key, value).build();
    }

    public MapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public Map<K, V> build() {
        return map;
    }

    public ImmutableMap<K, V> immutable() {
        return ImmutableMap.copyOf(map);
    }

    public static class StringMap extends MapBuilder<String, String> {
        @Override
        public StringMap put(String key, String value) {
            super.put(key, value);
            return this;
        }

        public StringMap putAny(String key, Object value) {
            super.put(key, String.valueOf(value));
            return this;
        }
    }
}
