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

package com.awakenedredstone.moondust.jankson.api;

import java.util.Set;

public final class Escaper {
    private static final Set<Character.UnicodeBlock> DEFAULT_BLOCKS = Set.of(Character.UnicodeBlock.BASIC_LATIN);

    private Escaper() {}

    public static String escapeString(String s) {
        return escapeString(s, '"');
    }

    public static String escapeString(String s, char quoteChar) {
        return escapeString(s, quoteChar, DEFAULT_BLOCKS);
    }

    /**
     * Escapes a string such that the result is valid as the contents of a java, js, or json string,
     * and the javascript unescape() function will restore the original string. Additionally, this
     * method attempts to do the minimum amount of escaping required to accomplish these goals.
     *
     * @param s         The String to escape special characters in
     * @param quoteChar the kind of quote used to delimit the String, either 0x22 (") or 0x27 ('). If you don't need quotes escaped, use 0x00.
     * @return A copy of the String, but with special characters escaped
     */
    public static String escapeString(String s, char quoteChar, Set<Character.UnicodeBlock> unquotedBlocks) {
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            //Encode easy stuff
            switch (ch) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\"':
                    if (quoteChar == ch) {
                        result.append("\\\"");
                    } else {
                        result.append(ch);
                    }
                    break;
                case '\'':
                    if (quoteChar == ch) {
                        result.append("\\'");
                    } else {
                        result.append(ch);
                    }
                    break;
                default:
                    Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
                    if (block != null) {
                        //Use Unicode notation if it's not especially printable - lies in a special Unicode block, is a control character, etc.
                        if (ch != 65535 && !Character.isISOControl(ch) && unquotedBlocks.contains(block)) {
                            //Note: 65535 is the value of awt's KeyEvent.CHARACTER_UNDEFINED. Just in case it leaks into a document.
                            result.append(ch);
                        } else {
                            result.append(unicodeEscape(ch));
                        }
                    } else {
                        //Always use Unicode notation
                        i++;
                        char upper = s.charAt(i);
                        int codePoint = Character.toCodePoint(ch, upper);
                        result.append(unicodeEscape(codePoint));
                    }
                    break;
            }
        }
        return result.toString();
    }

    private static String unicodeEscape(int codePoint) {
        StringBuilder codeString = new StringBuilder("\\u");
        codeString.append(Integer.toHexString(codePoint));
        while (codeString.length() < 4) codeString.insert(0, "0");
        return codeString.toString();
    }
}
