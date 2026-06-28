package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.moondust.config.api.ConfigValues;
import com.awakenedredstone.moondust.jankson.annotation.NameFormat;
import com.awakenedredstone.moondust.jankson.annotation.Comment;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.NullMarked;

import static com.awakenedredstone.autowhitelist.util.data.ModData.isModLoaded;

@NullMarked
@NameFormat(NameFormat.Case.SNAKE_CASE)
public class AutoWhitelistConfig extends ConfigValues.Codec {
    public AutoWhitelistConfig() {
        register("guyser_support", StringRepresentable.fromEnum(GuyserMode::values));
    }

    @Comment("""
      Configure GuyserMC support.
      Available options are "enabled", "disabled" and "auto". Defaults to "auto".
    """)
    public GuyserMode guyserSupport = GuyserMode.AUTO;

    @Comment("The options for the discord application")
    public DiscordConfig discord = new DiscordConfig();

    @Comment("Options for vanity features, it is recommended to leave this options unchanged.")
    public VanityConfig vanity = new VanityConfig();

    // @Nullable
    // @Comment("Options for Multilink, refer to the docs for setup.")
    // public MultilinkConfig multilink = null;

    @Comment("The options for the whitelist handling and behaviour")
    public WhitelistConfig whitelist = new WhitelistConfig();

    public MultilinkConfig multilink() {
        throw new NotImplementedException("Multilink is not fully implemented yet");
        // return Objects.requireNonNull(multilink, "Multilink config missing when it was required");
    }

    public enum GuyserMode implements StringRepresentable {
        AUTO(isModLoaded("geyser-fabric") && isModLoaded("floodgate")),
        DISABLED(false),
        ENABLED(true);

        public final boolean useGuyser;

        GuyserMode(boolean useGuyser) {
            this.useGuyser = useGuyser;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
