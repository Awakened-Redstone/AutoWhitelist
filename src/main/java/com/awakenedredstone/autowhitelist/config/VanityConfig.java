package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.moondust.jankson.annotation.Comment;
import com.awakenedredstone.moondust.jankson.annotation.NameFormat;

@NameFormat(NameFormat.Case.SNAKE_CASE)
public class VanityConfig {
    @Comment("The service url template used to render the player skins. Use %s where the player name should be placed at")
    public String playerRenderer = "https://vzge.me/bust/256/%s";

    @Comment("The skin to be used for players whose skin can't be fetched. This value will be passed instead the same as the \"player name\" in the player renderer.")
    public String unknownSkin = "d26eb011bc9f4cb22f8654ec71602d74cefa776c7a8607e94280ab6df4163d51";
}
