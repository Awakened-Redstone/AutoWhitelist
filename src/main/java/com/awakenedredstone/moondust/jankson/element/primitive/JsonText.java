package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;
import com.awakenedredstone.moondust.jankson.api.Escaper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

public final class JsonText extends JsonPrimitive<CharSequence> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in text.
     *
     * @param value The text value for the node
     */
    public JsonText(@NotNull CharSequence value) {
        super(value);
    }

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        writer.write('\"');
        writer.write(Escaper.escapeString(value.toString())); //TODO: Configurable unicode blocks to escape?
        writer.write('\"');
    }

    @Override
    public JsonText clone() {
        return new JsonText(value);
    }
}
