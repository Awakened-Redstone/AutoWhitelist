package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;

import java.io.IOException;
import java.io.Writer;

public sealed class JsonNumber<T extends Number> extends JsonPrimitive<T> permits JsonBigDecimal, JsonBigInteger, JsonByte, JsonDouble, JsonFloat, JsonInteger, JsonLong, JsonShort {
    /*
     * Creates a new JsonPrimitive node representing the passed-in number.
     *
     * @param value The number value for the node
     */
    public JsonNumber(T value) {
        super(value);
    }

    @Override
    public boolean isText() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        writer.write(value.toString());
    }

    @Override
    public JsonNumber<T> clone() {
        return new JsonNumber<>(value);
    }
}
