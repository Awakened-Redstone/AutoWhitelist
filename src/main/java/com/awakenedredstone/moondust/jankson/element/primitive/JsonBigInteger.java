package com.awakenedredstone.moondust.jankson.element.primitive;

import java.math.BigInteger;

public final class JsonBigInteger extends JsonNumber<BigInteger> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in BigInteger.
     *
     * @param value The BigInteger value for the node
     */
    public JsonBigInteger(BigInteger value) {
        super(value);
    }

    @Override
    public JsonBigInteger clone() {
        return new JsonBigInteger(value);
    }
}
