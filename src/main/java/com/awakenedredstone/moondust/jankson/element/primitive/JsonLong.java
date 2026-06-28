package com.awakenedredstone.moondust.jankson.element.primitive;

public final class JsonLong extends JsonNumber<Long> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in long.
     *
     * @param value The long value for the node
     */
    public JsonLong(long value) {
        super(value);
    }

    @Override
    public JsonLong clone() {
        return new JsonLong(value);
    }
}
