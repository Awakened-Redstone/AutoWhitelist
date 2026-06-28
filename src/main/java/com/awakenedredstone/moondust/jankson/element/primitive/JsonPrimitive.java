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

package com.awakenedredstone.moondust.jankson.element.primitive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import com.awakenedredstone.moondust.jankson.element.JsonElement;
import com.awakenedredstone.moondust.jankson.JsonGrammar;
import org.jetbrains.annotations.NotNull;

public sealed abstract class JsonPrimitive<T> extends JsonElement permits JsonBoolean, JsonCharacter, JsonNumber, JsonText {
    @NotNull
    protected final T value;

    /**
     * Creates a new JsonPrimitive node representing the passed-in value.
     *
     * @param value The value for the node
     */
    protected JsonPrimitive(@NotNull T value) {
        this.value = value;
    }

    //region As JSON nodes
    @NotNull
    public JsonText asTextNode() {
        if (this instanceof JsonText primitive) {
            return primitive;
        }

        return new JsonText(value.toString());
    }

    @NotNull
    public JsonBoolean asBooleanNode(boolean defaultValue) {
        if (value instanceof JsonBoolean primitive) {
            return primitive;
        }

        return new JsonBoolean(defaultValue);
    }

    @NotNull
    public JsonByte asByteNode(byte defaultValue) {
        if (value instanceof JsonByte primitive) {
            return primitive;
        }

        return new JsonByte(defaultValue);
    }

    @NotNull
    public JsonCharacter asCharNode(char defaultValue) {
        if (value instanceof JsonCharacter primitive) {
            return primitive;
        }

        return new JsonCharacter(defaultValue);
    }

    @NotNull
    public JsonShort asShortNode(short defaultValue) {
        if (value instanceof JsonShort primitive) {
            return primitive;
        }

        return new JsonShort(defaultValue);
    }

    @NotNull
    public JsonInteger asIntNode(int defaultValue) {
        if (value instanceof JsonInteger primitive) {
            return primitive;
        }

        return new JsonInteger(defaultValue);
    }

    @NotNull
    public JsonLong asLongNode(long defaultValue) {
        if (value instanceof JsonLong primitive) {
            return primitive;
        }

        return new JsonLong(defaultValue);
    }

    @NotNull
    public JsonFloat asFloatNode(float defaultValue) {
        if (value instanceof JsonFloat primitive) {
            return primitive;
        }

        return new JsonFloat(defaultValue);
    }

    @NotNull
    public JsonDouble asDoubleNode(double defaultValue) {
        if (value instanceof JsonDouble primitive) {
            return primitive;
        }

        return new JsonDouble(defaultValue);
    }

    @NotNull
    public JsonBigInteger asBigIntegerNode(BigInteger defaultValue) {
        if (value instanceof JsonBigInteger primitive) {
            return primitive;
        }

        return new JsonBigInteger(defaultValue);
    }

    @NotNull
    public JsonBigDecimal asBigDecimalNode(BigDecimal defaultValue) {
        if (value instanceof JsonBigDecimal primitive) {
            return primitive;
        }

        return new JsonBigDecimal(defaultValue);
    }
    //endregion

    //region As types
    @NotNull
    public String asString() {
        return value.toString();
    }

    public char asChar(char defaultValue) {
        switch (value) {
            case Number number -> {
                return (char) number.intValue();
            }
            case Character c -> {
                return c;
            }
            case CharSequence s -> {
                if (s.length() == 1) {
                    return s.charAt(0);
                } else {
                    return defaultValue;
                }
            }
            default -> {
                return defaultValue;
            }
        }
    }

    public boolean asBoolean(boolean defaultValue) {
        if (value instanceof JsonBoolean primitive) {
            return primitive.value;
        }

        return defaultValue;
    }

    public byte asByte(byte defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else {
            return defaultValue;
        }
    }

    public short asShort(short defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        } else {
            return defaultValue;
        }
    }

    public int asInt(int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return defaultValue;
        }
    }

    public long asLong(long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return defaultValue;
        }
    }

    public float asFloat(float defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else {
            return defaultValue;
        }
    }

    public double asDouble(double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return defaultValue;
        }
    }

    public Number asNumber(Number defaultValue) {
        if (value instanceof Number number) {
            return number;
        } else {
            return defaultValue;
        }
    }

    @NotNull
    public BigInteger asBigInteger(BigInteger defaultValue) {
        if (value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        } else if (value instanceof String) {
            return new BigInteger((String) value, 16);
        } else {
            return defaultValue;
        }
    }

    @NotNull
    public BigDecimal asBigDecimal(BigDecimal defaultValue) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        } else {
            return defaultValue;
        }
    }
    //endregion

    public abstract boolean isText();

    public abstract boolean isNumber();

    public abstract boolean isBoolean();

    @NotNull
    public T value() {
        return value;
    }

    @Override
    public String toJson(boolean comments, boolean newlines, int depth) {
        return toJson(JsonGrammar.builder().withComments(comments).printWhitespace(newlines).build(), depth);
    }

    /**
     * Creates a copy of this primitive. The referenced value is still the same.
     *
     * @return a copy of this primitive node
     */
    @Override
    public abstract JsonPrimitive<T> clone();

    @NotNull
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof JsonPrimitive<?> primitive) {
            return Objects.equals(value, primitive.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
