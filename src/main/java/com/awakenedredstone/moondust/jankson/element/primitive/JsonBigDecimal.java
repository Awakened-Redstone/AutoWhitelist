package com.awakenedredstone.moondust.jankson.element.primitive;

import java.math.BigDecimal;

public final class JsonBigDecimal extends JsonNumber<BigDecimal> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in BigDecimal.
     *
     * @param value The BigDecimal value for the node
     */
    public JsonBigDecimal(BigDecimal value) {
        super(value);
    }

    @Override
    public JsonBigDecimal clone() {
        return new JsonBigDecimal(value);
    }
}
