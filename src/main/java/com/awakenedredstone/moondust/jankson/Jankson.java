/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.awakenedredstone.moondust.jankson;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.awakenedredstone.moondust.jankson.api.DeserializationException;
import com.awakenedredstone.moondust.jankson.api.DeserializerFunction;
import com.awakenedredstone.moondust.jankson.api.Marshaller;
import com.awakenedredstone.moondust.jankson.api.SyntaxException;
import com.awakenedredstone.moondust.jankson.element.JsonElement;
import com.awakenedredstone.moondust.jankson.element.JsonNull;
import com.awakenedredstone.moondust.jankson.element.JsonObject;
import com.awakenedredstone.moondust.jankson.impl.*;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("DuplicatedCode")
public class Jankson {
    private final Deque<ParserFrame<?>> contextStack = new ArrayDeque<>();
    private JsonObject root;
    private int line = 0;
    private int column = 0;
    private int withheldCodePoint = -1;
    private Marshaller marshaller = MoonDustMarshaller.getFallback();
    private boolean allowBareRootObject = false;

    private int retries = 0;
    private SyntaxException delayedError = null;

    private Jankson(Builder builder) {
    }

    @NotNull
    public JsonObject load(String s) throws SyntaxException {
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        try {
            return load(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex); //ByteArrayInputStream never throws
        }
    }

    @NotNull
    public JsonObject load(File f) throws IOException, SyntaxException {
        try (InputStream in = new FileInputStream(f)) {
            return load(in);
        }
    }

    @NotNull
    public JsonObject load(InputStream in) throws IOException, SyntaxException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);

        withheldCodePoint = -1;
        root = null;

        push(new ObjectParserContext(allowBareRootObject), (it) -> {
            root = it;
        });

        //int codePoint = 0;
        while (root == null) {
            if (delayedError != null) {
                throw delayedError;
            }

            if (withheldCodePoint != -1) {
                retries++;
                if (retries > 25) throw new IOException("Parser got stuck near line " + line + " column " + column);
                processCodePoint(withheldCodePoint);
            } else {
                //int inByte = getCodePoint(in);
                int inByte = reader.read();
                if (inByte == -1) {
                    //Walk up the stack sending EOF to things until either an error occurs or the stack completes
                    while (!contextStack.isEmpty()) {
                        ParserFrame<?> frame = contextStack.pop();
                        try {
                            frame.context.eof();
                            if (frame.context.isComplete()) {
                                frame.supply();
                            }
                        } catch (SyntaxException error) {
                            error.setStartParsing(frame.startLine, frame.startCol);
                            error.setEndParsing(line, column);
                            throw error;
                        }
                    }
                    if (root == null) {
                        root = new JsonObject();
                        root.setMarshaller(marshaller);
                    }
                    return root;
                }
                processCodePoint(inByte);
            }
        }

        return root;
    }

    /**
     * Experimental: Parses the supplied String as a JsonElement, which may or may not be an object at the root level
     */
    @NotNull
    public JsonElement loadElement(String s) throws SyntaxException {
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        try {
            return loadElement(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex); //ByteArrayInputStream never throws
        }
    }

    /**
     * Experimental: Parses the supplied File as a JsonElement, which may or may not be an object at the root level
     */
    @NotNull
    public JsonElement loadElement(File f) throws IOException, SyntaxException {
        try (InputStream in = new FileInputStream(f)) {
            return loadElement(in);
        }
    }

    private AnnotatedElement rootElement;

    /**
     * Experimental: Parses the supplied InputStream as a JsonElement, which may or may not be an object at the root level
     */
    @NotNull
    public JsonElement loadElement(InputStream in) throws IOException, SyntaxException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);

        withheldCodePoint = -1;
        rootElement = null;

        push(new ElementParserContext(), (it) -> {
            rootElement = it;
        });

        //int codePoint = 0;
        while (rootElement == null) {
            if (delayedError != null) {
                throw delayedError;
            }

            if (withheldCodePoint != -1) {
                retries++;
                if (retries > 25) throw new IOException("Parser got stuck near line " + line + " column " + column);
                processCodePoint(withheldCodePoint);
            } else {
                //int inByte = getCodePoint(in);
                int inByte = reader.read();
                if (inByte == -1) {
                    //Walk up the stack sending EOF to things until either an error occurs or the stack completes
                    while (!contextStack.isEmpty()) {
                        ParserFrame<?> frame = contextStack.pop();
                        try {
                            frame.context.eof();
                            if (frame.context.isComplete()) {
                                frame.supply();
                            }
                        } catch (SyntaxException error) {
                            error.setStartParsing(frame.startLine, frame.startCol);
                            error.setEndParsing(line, column);
                            throw error;
                        }
                    }
                    if (rootElement == null) {
                        return JsonNull.INSTANCE;
                    } else {
                        return rootElement.getElement();
                    }
                } else {
                    processCodePoint(inByte);
                }
            }
        }

        return rootElement.getElement();
    }

    public <T> T fromJson(JsonObject obj, Class<T> clazz) {
        return marshaller.marshall(clazz, obj);
    }

    public <T> T fromJson(String json, Class<T> clazz) throws SyntaxException {
        JsonObject obj = load(json);
        return fromJson(obj, clazz);
    }

    /**
     * Converts a String of json into an object of the specified class in fail-fast mode, throwing an exception
     * proactively if problems arise.
     *
     * @param json  A string containing json data to be unpacked
     * @param clazz The class to convert the data into
     * @return An object representing the data in json
     * @throws SyntaxException              If the json cannot be parsed
     * @throws DeserializationException If the conversion into an instance of the specified type fails
     */
    public <T> T fromJsonCarefully(String json, Class<T> clazz) throws SyntaxException, DeserializationException {
        JsonObject obj = load(json);
        return fromJsonCarefully(obj, clazz);
    }

    /**
     * Converts a JsonObject into an object of the specified class, in fail-fast mode, throwing an exception
     * proactively if problems arise
     *
     * @param obj   A JsonObject holding the data to be unpacked
     * @param clazz The class to convert the data into
     * @return An object of the specified class, holding the data from json
     * @throws DeserializationException If the conversion into an instance of the specified type fails
     */
    public <T> T fromJsonCarefully(JsonObject obj, Class<T> clazz) throws DeserializationException {
        return marshaller.marshallCarefully(clazz, obj);
    }

    public <T> JsonElement toJson(T t) {
        return marshaller.serialize(t);
    }

    public <T> JsonElement toJson(T t, Marshaller alternateMarshaller) {
        return alternateMarshaller.serialize(t);
    }

    private void processCodePoint(int codePoint) throws SyntaxException {
        ParserFrame<?> frame = contextStack.peek();
        if (frame == null) throw new IllegalStateException("Parser problem! The ParserContext stack underflowed! (line " + line + ", col " + column + ")");

        //Do a limited amount of tail call recursion
        try {
            if (frame.context().isComplete()) {
                contextStack.pop();
                frame.supply();
                frame = contextStack.peek();
            }
        } catch (SyntaxException error) {
            error.setStartParsing(frame.startLine, frame.startCol);
            error.setEndParsing(line, column);
            throw error;
        }

        try {
            if (frame == null) return; //We ran out of stack frames due to tail call recursion. This means the file's done.
            boolean consumed = frame.context().consume(codePoint, this);
            if (frame.context.isComplete()) {
                contextStack.pop();
                frame.supply();
            }
            if (consumed) {
                withheldCodePoint = -1;
                retries = 0;
            } else {
                withheldCodePoint = codePoint;
            }

        } catch (SyntaxException error) {
            error.setStartParsing(frame.startLine, frame.startCol);
            error.setEndParsing(line, column);
            throw error;
        }

        column++;
        if (codePoint == '\n') {
            line++;
            column = 0;
        }
    }


    /**
     * Pushes a context onto the stack. MAY ONLY BE CALLED BY THE ACTIVE CONTEXT
     */
    public <T> void push(ParserContext<T> t, Consumer<T> consumer) {
        ParserFrame<T> frame = new ParserFrame<T>(t, consumer);
        frame.startLine = line;
        frame.startCol = column;
        contextStack.push(frame);
    }

    public Marshaller getMarshaller() {
        return marshaller;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        protected final MoonDustMarshaller marshaller = new MoonDustMarshaller();
        boolean allowBareRootObject = false;

        /**
         * Registers a codec to serialize and deserialize an object. This can be useful if a class's serialized form is not
         * meant to resemble its live-memory form.
         *
         * @param clazz The class to register a codec for
         * @param codec The codec to be used to serialize and deserialize the data
         * @return This Builder for further modification.
         */
        public <T> Builder registerCodec(Class<T> clazz, Codec<T> codec) {
            marshaller.registerCodec(clazz, codec);
            return this;
        }

        /**
         * Registers a function to serialize an object into json. This can be useful if a class's serialized form is not
         * meant to resemble its live-memory form.
         *
         * @param clazz      The class to register a serializer for
         * @param serializer A function which takes the object and a Marshaller, and produces a serialized JsonElement
         * @return This Builder for further modificaton.
         */
        public <T> Builder registerSerializer(Class<T> clazz, BiFunction<T, Marshaller, JsonElement> serializer) {
            marshaller.registerSerializer(clazz, serializer);
            return this;
        }

        public <A, B> Builder registerDeserializer(Class<A> sourceClass, Class<B> targetClass, DeserializerFunction<A, B> function) {
            marshaller.registerDeserializer(sourceClass, targetClass, function);
            return this;
        }

        /**
         * Registers a factory that can generate empty objects of the specified type. Sometimes it's not practical
         * to have a no-arg constructor available on an object, so the function to create blanks can be specified
         * here.
         *
         * @param clazz   The class to use an alternate factory for
         * @param factory A Supplier which can create blank objects of class `clazz` for deserialization
         * @return This Builder for further modification.
         */
        public <T> Builder registerTypeFactory(Class<T> clazz, Supplier<T> factory) {
            marshaller.registerTypeFactory(clazz, factory);
            return this;
        }

        /**
         * Allows loading JSON files that do not contain root braces, as generated with
         * {@link JsonGrammar.Builder#bareRootObject(boolean) bareRootObject}.
         *
         * @return This Builder for further modification.
         */
        public Builder allowBareRootObject() {
            allowBareRootObject = true;
            return this;
        }

        public Jankson build() {
            Jankson result = new Jankson(this);
            result.marshaller = marshaller;
            result.allowBareRootObject = allowBareRootObject;
            return result;
        }
    }

    private static class ParserFrame<T> {
        private final ParserContext<T> context;
        private final Consumer<T> consumer;
        private int startLine = 0;
        private int startCol = 0;

        public ParserFrame(ParserContext<T> context, Consumer<T> consumer) {
            this.context = context;
            this.consumer = consumer;
        }

        public ParserContext<T> context() {
            return context;
        }
        //public Consumer<T> consumer() { return consumer; }

        /**
         * Feed the result directly from the context at this entry to its corresponding consumer
         */
        public void supply() throws SyntaxException {
            consumer.accept(context.getResult());
        }
    }

    public void throwDelayed(SyntaxException syntaxException) {
        syntaxException.setEndParsing(line, column);
        delayedError = syntaxException;
    }
}
