package com.awakenedredstone.moondust.jankson.element.primitive;

public final class JsonByte extends JsonNumber<Byte> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in byte.
     *
     * @param value The byte for the node
     */
    public JsonByte(byte value) {
        super(value);
    }

    @Override
    public JsonByte clone() {
        return new JsonByte(value);
    }
}
