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

package com.awakenedredstone.moondust.jankson.impl;

import java.util.Locale;

import com.awakenedredstone.moondust.jankson.Jankson;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonDouble;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonLong;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonNumber;
import com.awakenedredstone.moondust.jankson.api.SyntaxException;

public class NumberParserContext implements ParserContext<JsonNumber<?>> {
    private static final String ACCEPTED_CHARS = "0123456789.+-eExabcdefInityNnABCDF";
    private String numberString = "";
    private boolean complete = false;

    public NumberParserContext(int firstCodePoint) {
        numberString += (char) firstCodePoint;
    }

    @Override
    public boolean consume(int codePoint, Jankson loader) throws SyntaxException {
        if (complete) return false;

        if (ACCEPTED_CHARS.indexOf(codePoint) != -1) {
            numberString += (char) codePoint;
            return true;
        } else {
            complete = true;
            return false;
        }
    }

    @Override
    public void eof() {
        complete = true;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public JsonNumber<?> getResult() throws SyntaxException {
        //parse special values
        String lowerCase = numberString.toLowerCase(Locale.ROOT);
        switch (lowerCase) {
            case "infinity", "+infinity" -> {
                return new JsonDouble(Double.POSITIVE_INFINITY);
            }
            case "-infinity" -> {
                return new JsonDouble(Double.NEGATIVE_INFINITY);
            }
            case "nan" -> {
                return new JsonDouble(Double.NaN);
            }
        }

        //Fallback to the number parsers
        if (numberString.startsWith(".")) numberString = '0' + numberString;
        if (numberString.endsWith(".")) numberString = numberString + '0';
        if (numberString.startsWith("0x")) {
            numberString = numberString.substring(2);
            try {
                long number = Long.parseUnsignedLong(numberString, 16);
                return new JsonLong(number);
            } catch (NumberFormatException nfe) {
                throw new SyntaxException("Tried to parse '" + numberString + "' as a hexadecimal number, but it appears to be invalid.");
            }
        }
        if (numberString.startsWith("-0x")) {
            numberString = numberString.substring(3);
            try {
                long number = -Long.parseUnsignedLong(numberString, 16);
                return new JsonLong(number);
            } catch (NumberFormatException nfe) {
                throw new SyntaxException("Tried to parse '" + numberString + "' as a hexadecimal number, but it appears to be invalid.");
            }
        }


        if (numberString.indexOf('.') != -1) {
            // Return as a Double
            try {
                double number = Double.parseDouble(numberString);
                return new JsonDouble(number);
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Tried to parse '" + numberString + "' as a floating-point number, but it appears to be invalid.");
            }
        } else {
            // Return as a Long
            try {
                long number = Long.parseLong(numberString);
                return new JsonLong(number);
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Tried to parse '" + numberString + "' as an integer, but it appears to be invalid.");
            }
        }
    }

}
