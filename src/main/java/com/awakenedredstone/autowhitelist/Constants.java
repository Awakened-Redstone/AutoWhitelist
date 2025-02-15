package com.awakenedredstone.autowhitelist;

import blue.endless.jankson.JsonGrammar;

public class Constants {
    /*
    1 - Initial config
    2 - Major refactor, use snake_case and renaming
    3 - Added `cache_discord_data`
    4 - Doesn't exist
    5 - Added `command_permission_level`
    6 - Removed `admins` and `prefix`, renamed `update_period` to `periodic_check_delay`
    */
    public static final byte CONFIG_VERSION = 6;
    public static final String CONFIG_SCHEMA = "https://awakenedredstone.com/json-schema/autowhitelist/draft-%02d.json".formatted(CONFIG_VERSION);
    public static final JsonGrammar GRAMMAR = JsonGrammar.builder()
      .withComments(true)
      .printTrailingCommas(false)
      .printWhitespace(true)
      .printUnquotedKeys(false)
      .bareSpecialNumerics(true)
      .build();
}
