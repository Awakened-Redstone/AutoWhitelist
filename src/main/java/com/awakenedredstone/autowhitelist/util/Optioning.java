package com.awakenedredstone.autowhitelist.util;

import com.awakenedredstone.autowhitelist.mixin.discord4j.ApplicationCommandInteractionOptionValueAccessor;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class Optioning {
    public static Optional<Mono<Member>> getOptionAsMember(@NonNull List<ApplicationCommandInteractionOption> options, String name) {
        var perhapsOption = getOption(options, name);
        if (perhapsOption.isEmpty() || perhapsOption.get().getValue().isEmpty()) {
            return Optional.empty();
        }

        var option = perhapsOption.get().getValue().get();
        var accessor = (ApplicationCommandInteractionOptionValueAccessor) option;
        if (accessor.getGuildId() == null) {
            return Optional.empty();
        }

        return Optional.of(
          accessor.callGetValueAs(
            "member",
            value -> option.getClient().getMemberById(Snowflake.of(accessor.getGuildId()), Snowflake.of(value)),
            ApplicationCommandOption.Type.USER
          )
        );
    }

    public static Optional<String> getOptionAsString(@NonNull List<ApplicationCommandInteractionOption> options, String name) {
        return getOption(options, name).flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asString);
    }

    /**
     * Gets the option corresponding to the provided name, if present.
     *
     * @param name The name of the option.
     * @return The option corresponding to the provided name, if present.
     */
    public static Optional<ApplicationCommandInteractionOption> getOption(@NonNull List<ApplicationCommandInteractionOption> options, final String name) {
        return options.stream().filter(option -> option.getName().equals(name)).findFirst();
    }
}
