package com.awakenedredstone.moondust.jankson.element.primitive;

public final class JsonInteger extends JsonNumber<Integer> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in integer.
     *
     * @param value The integer value for the node
     */
    public JsonInteger(int value) {
        super(value);
    }

    @Override
    public JsonInteger clone() {
        return new JsonInteger(value);
    }
}
