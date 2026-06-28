package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;

import java.io.IOException;
import java.io.Writer;

public final class JsonFloat extends JsonNumber<Float> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in float.
     *
     * @param value The float value for the node
     */
    public JsonFloat(float value) {
        super(value);
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        if (Float.isNaN(value)) {
            writer.write("NaN");
            return;
        }
        if (Float.isInfinite(value)) {
            writer.write(value < 0 ? "-Infinity" : "Infinity");
            return;
        }
        writer.write(value.toString());
    }

    @Override
    public JsonFloat clone() {
        return new JsonFloat(value);
    }
}
