package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * @param roles The roles allowed for this entry
 * @param actions The actions to be executed
 */
public record Entry(List<String> roles, List<EntryAction<?>> actions) {
    // TODO: maybe use a custom alternative codec with a simpler error message
    private static final Codec<List<EntryAction<?>>> ACTIONS_CODEC = Codec.withAlternative(EntryAction.CODEC.listOf(), EntryAction.CODEC.flatXmap(action -> DataResult.success(List.of(action)), list -> DataResult.success(list.getFirst())));

    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(Entry::roles),
        ACTIONS_CODEC.optionalFieldOf("actions", List.of()).forGetter(Entry::actions)
      ).apply(instance, Entry::new)
    );
}
