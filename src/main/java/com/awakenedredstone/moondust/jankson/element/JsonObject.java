/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.awakenedredstone.moondust.jankson.element;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.awakenedredstone.moondust.jankson.JsonGrammar;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonPrimitive;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonText;
import com.awakenedredstone.moondust.jankson.impl.MoonDustMarshaller;
import com.awakenedredstone.moondust.jankson.api.Marshaller;
import com.awakenedredstone.moondust.jankson.impl.serializer.CommentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObject extends JsonElement implements Map<String, JsonElement> {
    /**
     * This pattern matches JsonObject keys that are permitted to appear unquoted
     */
    private static final Predicate<String> CAN_BE_UNQUOTED = Pattern.compile("^[a-zA-Z0-9]+$").asPredicate();
    protected Marshaller marshaller = MoonDustMarshaller.getFallback();
    private final List<InnerEntry<String>> entries = new ArrayList<>();

    /**
     * If there is an entry at this key, and that entry is a json object, return it. Otherwise returns null.
     */
    @Nullable
    public JsonObject getObject(@NotNull String key) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                if (entry.getValue() instanceof JsonObject object) {
                    return object;
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    public JsonArray getArray(@NotNull String key) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                if (entry.getValue() instanceof JsonArray array) {
                    return array;
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Replaces a key-value mapping in this object if it exists, or adds the mapping to the end of the object if it
     * doesn't. Returns the old value mapped to this key if there was one.
     */
    public JsonElement put(@NotNull String key, @NotNull JsonElement elem, @Nullable InnerEntry.Meta meta) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                JsonElement result = entry.getValue();
                entry.setValue(elem);
                entry.setMeta(meta);
                return result;
            }
        }

        //If we reached here, there's no existing mapping, so make one.
        if (elem instanceof JsonObject) ((JsonObject) elem).marshaller = marshaller;
        if (elem instanceof JsonArray) ((JsonArray) elem).marshaller = marshaller;

        InnerEntry<String> entry = new InnerEntry<>(key, elem, meta);
        entries.add(entry);
        return null;
    }

    @NotNull
    public JsonElement putDefault(@NotNull String key, @NotNull JsonElement elem, @Nullable InnerEntry.Meta comment) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        //If we reached here, there's no existing mapping, so make one.
        InnerEntry<String> entry = new InnerEntry<>(key, elem, comment);
        entries.add(entry);
        return elem;
    }

    /**
     * May return null if the existing object can't be marshaled to elem's class
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T putDefault(@NotNull String key, @NotNull T elem, @Nullable InnerEntry.Meta meta) {
        return (T) putDefault(key, elem, elem.getClass(), meta);
    }

    /**
     * May return null if the existing object can't be marshaled to the target class
     */
    @Nullable
    public <T> T putDefault(@NotNull String key, @NotNull T elem, Class<? extends T> clazz, @Nullable InnerEntry.Meta meta) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return marshaller.marshall(clazz, entry.getValue());
            }
        }

        //If we reached here, there's no existing mapping, so make one.
        InnerEntry<String> entry = new InnerEntry<>(key, marshaller.serialize(elem), meta);
        entries.add(entry);
        return elem;
    }

    /**
     * Gets a minimal set of key-value-comment settings which, if added to the supplied JsonObject, would produce this
     * JsonObject. See BasicTests::testDiffAgainstDefaults() for more details on this comparison.
     *
     * <ul>
     *   <li>If a key is present in the default and not in the object, it's skipped
     *   <li>If a key is an object, a deep (recursive) comparison occurs. Comments are ignored in this comparison.
     *   <li>All other types, including lists, receive a shallow comparison of its value. The comment is ignored in this comparison.
     *   <li>Whether deep or shallow, if the key is found to be identical in value to its default, it is skipped.
     *   <li>If the key is found to be different than its default, the key, value, and comment are represented in the
     *       output.
     * </ul>
     */
    @NotNull
    public JsonObject getDelta(@NotNull JsonObject defaults) {
        JsonObject result = new JsonObject();
        for (InnerEntry<String> entry : entries) {
            String key = entry.getKey();
            JsonElement defaultValue = defaults.get(key);
            if (defaultValue == null) {
                result.put(entry.getKey(), entry.getValue(), entry.getMeta());
                continue;
            }

            if (entry.getValue() instanceof JsonObject) {
                if (defaultValue instanceof JsonObject) {
                    JsonObject subDelta = ((JsonObject) entry.getValue()).getDelta((JsonObject) defaultValue);
                    if (!subDelta.isEmpty()) {
                        result.put(entry.getKey(), subDelta, entry.getMeta());
                    }

                    continue;
                }
            }

            if (entry.getValue().equals(defaultValue)) continue;

            result.put(entry.getKey(), entry.getValue(), entry.getMeta());
        }

        return result;
    }

    /**
     * Returns the comment "attached to" a given key-value mapping, which is to say, the comment appearing immediately
     * before it or the single-line comment to the right of it.
     */
    @Nullable
    public InnerEntry.Meta getMeta(@NotNull String name) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getMeta();
            }
        }

        return null;
    }

    public void setMeta(@NotNull String name, @Nullable InnerEntry.Meta meta) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                entry.setMeta(meta);
                return;
            }
        }
    }

    @Override
    public String toJson(boolean comments, boolean newlines, int depth) {
        JsonGrammar grammar = JsonGrammar.builder().withComments(comments).printWhitespace(newlines).build();
        return toJson(grammar, depth);
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        boolean skipBraces = depth == 0 && grammar.bareRootObject();
        int effectiveDepth = (grammar.bareRootObject()) ? depth - 1 : depth;
        int nextDepth = (grammar.bareRootObject()) ? depth : depth + 1;

        if (!skipBraces) {
            writer.append("{");

            if (grammar.printWhitespace() && !entries.isEmpty()) {
                writer.append('\n');
            } else {
                writer.append(' ');
            }
        }

        for (int i = 0; i < entries.size(); i++) {
            InnerEntry<String> entry = entries.get(i);

            if (grammar.printWhitespace()) {
                for (int j = 0; j < nextDepth; j++) {
                    writer.append("\t");
                }
            }

            CommentSerializer.print(writer, entry.getMeta().getComment(), effectiveDepth, grammar);

            boolean quoted = !grammar.printUnquotedKeys();

            //If it can't be unquoted, quote it anyway
            if (!CAN_BE_UNQUOTED.test(entry.getKey())) {
                quoted = true;
            }

            if (quoted) writer.append("\"");
            writer.append(entry.getKey());
            if (quoted) writer.append("\"");

            writer.append(": ");
            if (entry.getMeta().isSecret() && !grammar.printSecrets()) {
                assert entry.getMeta().secretPlaceholder() != null;
                writer.append(new JsonText(entry.getMeta().secretPlaceholder()).toJson(grammar, depth + 1));
            } else {
                writer.append(entry.getValue().toJson(grammar, depth + 1));
            }

            if (grammar.printCommas()) {
                if (i < entries.size() - 1 || grammar.printTrailingCommas()) {
                    writer.append(",");
                    if (i < entries.size() - 1 && !grammar.printWhitespace()) writer.append(' ');
                }
            } else if (!grammar.printWhitespace()) {
                writer.append(" ");
            }

            if (grammar.printWhitespace()) {
                writer.append('\n');
            }
        }

        if (!skipBraces) {
            if (!entries.isEmpty()) {
                if (grammar.printWhitespace()) {
                    for (int j = 0; j < effectiveDepth; j++) {
                        writer.append("\t");
                    }
                } else {
                    writer.append(' ');
                }
            }

            writer.append("}");
        }
    }

    @Override
    public String toString() {
        return toJson(true, false, 0);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JsonObject otherObject)) return false;
        if (entries.size() != otherObject.entries.size()) return false;

        //Lists are identical sizes, but if the contents, comments, or ordering are at all different, fail them
        for (int i = 0; i < entries.size(); i++) {
            InnerEntry<String> a = entries.get(i);
            InnerEntry<String> b = otherObject.entries.get(i);

            if (!a.equals(b)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public Marshaller getMarshaller() {
        return this.marshaller;
    }

    @Nullable
    public <E> E get(@NotNull Class<E> clazz, @NotNull String key) {
        if (key.isEmpty()) throw new IllegalArgumentException("Cannot get from empty key");

        JsonElement elem = get(key);
        return marshaller.marshall(clazz, elem);
    }

    //Convenience getters

    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asBoolean(defaultValue);
        }
        return defaultValue;
    }

    public byte getByte(@NotNull String key, byte defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asByte(defaultValue);
        }
        return defaultValue;
    }

    public char getChar(@NotNull String key, char defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asChar(defaultValue);
        }
        return defaultValue;
    }

    public short getShort(@NotNull String key, short defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asShort(defaultValue);
        }
        return defaultValue;
    }

    public int getInt(@NotNull String key, int defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asInt(defaultValue);
        }
        return defaultValue;
    }

    public long getLong(@NotNull String key, long defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asLong(defaultValue);
        }
        return defaultValue;
    }

    public float getFloat(@NotNull String key, float defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asFloat(defaultValue);
        }
        return defaultValue;
    }

    public double getDouble(@NotNull String key, double defaultValue) {
        JsonElement elem = get(key);
        if (elem instanceof JsonPrimitive) {
            return ((JsonPrimitive<?>) elem).asDouble(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Gets a (potentially nested) element from this object if it exists.
     *
     * @param clazz The expected class of the element
     * @param key   The keys of the nested elements, separated by periods, such as "foo.bar.baz"
     * @return The element at that location, if it exists and is of the proper type, otherwise null.
     */
    @Nullable
    public <E> E recursiveGet(@NotNull Class<E> clazz, @NotNull String key) {
        if (key.isEmpty()) throw new IllegalArgumentException("Cannot get from empty key");
        String[] parts = key.split("\\.");
        JsonObject cur = this;
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i];
            if (s.isEmpty()) throw new IllegalArgumentException("Cannot get from broken key '" + key + "'");
            JsonElement elem = cur.get(s);
            if (i < parts.length - 1) {
                //elem must be a JsonObject or we're sunk
                if (elem instanceof JsonObject) {
                    cur = (JsonObject) elem;
                    continue;
                } else {
                    return null;
                }
            } else {
                return marshaller.marshall(clazz, elem);
            }
        }
        throw new IllegalArgumentException("Cannot get from broken key '" + key + "'");
    }

    /**
     * Gets a (potentially nested) element from this object if it exists, or creates it and any intermediate objects
     * needed to put it at the indicated location in the hierarchy.
     *
     * @param clazz The expected class of the element
     * @param key   The keys of the nested elements, separated by periods, such as "foo.bar.baz"
     * @return The element at that location if it exists, or the newly-created element if it did not previously exist.
     */
    @SuppressWarnings("unchecked")
    public <E extends JsonElement> E recursiveGetOrCreate(@NotNull Class<E> clazz, @NotNull String key, @NotNull E fallback, @Nullable InnerEntry.Meta meta) {
        if (key.isEmpty()) throw new IllegalArgumentException("Cannot get from empty key");
        String[] parts = key.split("\\.");
        JsonObject cur = this;
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i];
            if (s.isEmpty()) throw new IllegalArgumentException("Cannot get from broken key '" + key + "'");
            JsonElement elem = cur.get(s);
            if (i < parts.length - 1) {
                //elem must be a JsonObject or we're sunk
                if (elem instanceof JsonObject) {
                    cur = (JsonObject) elem;
                    continue;
                }

                JsonObject replacement = new JsonObject();
                cur.put(s, replacement);
                cur = replacement;
            } else {
                if (elem != null && clazz.isAssignableFrom(elem.getClass())) {
                    return (E) elem;
                }

                E result = (E) fallback.clone();
                cur.put(s, result, meta);
                return result;
            }
        }

        throw new IllegalArgumentException("Cannot get from broken key '" + key + "'");
    }


    private static final class Entry {
        private String comment;
        private String key;
        private JsonElement value;

        public Entry() {

        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Entry entry)) return false;
            if (!Objects.equals(comment, entry.comment)) return false;
            if (!key.equals(entry.key)) return false;

            return value.equals(entry.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(comment, key, value);
        }

        public String getComment() {
            return this.comment;
        }

        public void setComment(String comment) {
            if (comment != null && !comment.trim().isEmpty()) {
                this.comment = comment;
            } else {
                this.comment = null;
            }
        }
    }

    //implements Cloneable {

    @Override
    public JsonObject clone() {
        JsonObject result = new JsonObject();
        for (InnerEntry<String> entry : entries) {
            result.put(entry.getKey(), entry.getValue().clone(), entry.getMeta());
        }
        result.marshaller = marshaller;
        return result;
    }

    //}

    //implements Map<JsonElement> {

    /**
     * Replaces a key-value mapping in this object if it exists, or adds the mapping to the end of the object if it
     * doesn't. Returns the old value mapped to this key if there was one.
     */
    @Override
    @Nullable
    public JsonElement put(@NotNull String key, @NotNull JsonElement elem) {
        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                JsonElement result = entry.getValue();
                entry.setValue(elem);
                return result;
            }
        }

        //If we reached here, there's no existing mapping, so make one.
        InnerEntry<String> entry = new InnerEntry<>(key, elem);
        entries.add(entry);
        return null;
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        if (key == null) return false;
        if (!(key instanceof String)) return false;

        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase((String) key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsValue(@Nullable Object val) {
        if (val == null) return false;
        if (!(val instanceof JsonElement)) return false;

        for (InnerEntry<String> entry : entries) {
            if (entry.getValue().equals(val)) return true;
        }

        return false;
    }

    /**
     * Creates a semi-live shallow copy instead of a live view
     */
    @Override
    @NotNull
    public Set<Map.Entry<String, JsonElement>> entrySet() {
        Set<Map.Entry<String, JsonElement>> result = new LinkedHashSet<>();
        for (InnerEntry<String> entry : entries) {
            result.add(new Map.Entry<>() {
                @Override
                public String getKey() {
                    return entry.getKey();
                }

                @Override
                public JsonElement getValue() {
                    return entry.getValue();
                }

                @Override
                public JsonElement setValue(JsonElement value) {
                    JsonElement oldValue = entry.getValue();
                    entry.setValue(value);
                    return oldValue;
                }

            });
        }

        return result;
    }

    @Override
    @Nullable
    public JsonElement get(@Nullable Object key) {
        if (!(key instanceof String)) return null;

        for (InnerEntry<String> entry : entries) {
            if (entry.getKey().equalsIgnoreCase((String) key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns a defensive copy instead of a live view
     */
    @Override
    @NotNull
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (InnerEntry<String> entry : entries) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonElement> map) {
        for (Map.Entry<? extends String, ? extends JsonElement> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Nullable
    public JsonElement remove(@Nullable Object key) {
        if (!(key instanceof String)) return null;

        for (int i = 0; i < entries.size(); i++) {
            InnerEntry<String> entry = entries.get(i);
            if (entry.getKey().equalsIgnoreCase((String) key)) {
                return entries.remove(i).getValue();
            }
        }
        return null;
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    @NotNull
    public Collection<JsonElement> values() {
        List<JsonElement> values = new ArrayList<>();
        for (InnerEntry<String> entry : entries) {
            values.add(entry.getValue());
        }
        return values;
    }
}
