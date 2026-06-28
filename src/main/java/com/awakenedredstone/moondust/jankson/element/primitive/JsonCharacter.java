package com.awakenedredstone.moondust.jankson.element.primitive;

import com.awakenedredstone.moondust.jankson.JsonGrammar;
import com.awakenedredstone.moondust.jankson.api.Escaper;

import java.io.IOException;
import java.io.Writer;

public final class JsonCharacter extends JsonPrimitive<Character> {
    /**
     * Creates a new JsonPrimitive node representing the passed-in character.
     *
     * @param value The character value for the node
     */
    public JsonCharacter(char value) {
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
        writer.write('\'');
        writer.write(Escaper.escapeString(value.toString(), '\''));
        writer.write('\'');
    }

    @Override
    public JsonCharacter clone() {
        return new JsonCharacter(value);
    }
}
