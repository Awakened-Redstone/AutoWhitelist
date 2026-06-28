package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;

import java.io.IOException;
import java.io.Writer;

public final class JsonDouble extends JsonNumber<Double> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in double.
     *
     * @param value The double value for the node
     */
    public JsonDouble(double value) {
        super(value);
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        if (Double.isNaN(value)) {
            writer.write("NaN");
            return;
        }
        if (Double.isInfinite(value)) {
            writer.write(value < 0 ? "-Infinity" : "Infinity");
            return;
        }
        writer.write(value.toString());
    }

    @Override
    public JsonDouble clone() {
        return new JsonDouble(value);
    }
}
