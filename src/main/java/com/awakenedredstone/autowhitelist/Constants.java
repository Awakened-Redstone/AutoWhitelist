package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonGrammar;

public class Constants {
    public static final byte CONFIG_VERSION = 3;
    public static final String CONFIG_SCHEMA = "https://awakenedredstone.com/json-schema/autowhitelist/draft-%02d.json".formatted(CONFIG_VERSION);
    public static final JsonGrammar GRAMMAR = JsonGrammar.builder()
      .withComments(true)
      .printTrailingCommas(false)
      .printWhitespace(true)
      .printUnquotedKeys(false)
      .bareSpecialNumerics(true)
      .build();
}
