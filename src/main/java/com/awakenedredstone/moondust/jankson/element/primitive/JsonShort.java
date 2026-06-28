package com.awakenedredstone.moondust.jankson.element.primitive;

public final class JsonShort extends JsonNumber<Short> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in short.
     *
     * @param value The short value for the node
     */
    public JsonShort(short value) {
        super(value);
    }

    @Override
    public JsonShort clone() {
        return new JsonShort(value);
    }
}
