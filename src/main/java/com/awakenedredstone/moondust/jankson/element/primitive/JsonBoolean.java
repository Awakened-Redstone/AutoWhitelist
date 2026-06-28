package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;

import java.io.IOException;
import java.io.Writer;

public final class JsonBoolean extends JsonPrimitive<Boolean> {
    /**
     * Convenience instance of json "true". Don't use identity comparison (==) on these! Use equals instead.
     */
    public static final JsonBoolean TRUE = new JsonBoolean(true);
    /**
     * Convenience instance of json "false". Don't use identity comparison (==) on these! Use equals instead.
     */
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    /**
     * Creates a new JsonPrimitive node representing the passed-in boolean.
     *
     * @param value The boolean value for the node
     */
    public JsonBoolean(boolean value) {
        super(value);
    }

    @Override
    public boolean isText() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        writer.write(value.toString());
    }

    @Override
    public JsonBoolean clone() {
        return new JsonBoolean(value);
    }
}
