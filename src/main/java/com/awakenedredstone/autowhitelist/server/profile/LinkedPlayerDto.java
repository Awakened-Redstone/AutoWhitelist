package com.awakenedredstone.autowhitelist.server.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.players.NameAndId;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LinkedPlayerDto extends PlayerDto {
    private static final MethodHandle EQUALS;

    public static final MapCodec<LinkedPlayerDto> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
        UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(LinkedPlayerDto::id),
        Codec.STRING.optionalFieldOf("name").forGetter(LinkedPlayerDto::name),
        Codec.STRING.optionalFieldOf("discordId").forGetter(LinkedPlayerDto::discordId),
        Codec.STRING.optionalFieldOf("role").forGetter(LinkedPlayerDto::role),
        Codec.LONG.optionalFieldOf("role").forGetter(LinkedPlayerDto::lockedUntil)
      ).apply(instance, LinkedPlayerDto::new)
    );
    private final Optional<String> discordId;
    private final Optional<String> role;
    private final Optional<Long> lockedUntil;

    public LinkedPlayerDto(Optional<UUID> id, Optional<String> name, Optional<String> discordId, Optional<String> role, Optional<Long> lockedUntil) {
        super(id, name);
        this.discordId = discordId;
        this.role = role;
        this.lockedUntil = lockedUntil;
    }

    public static LinkedPlayerDto from(NameAndId nameAndId) {
        if (nameAndId instanceof LinkedPlayerProfile linkedProfile) {
            return new LinkedPlayerDto(
              Optional.of(linkedProfile.id()),
              Optional.of(linkedProfile.name()),
              Optional.of(linkedProfile.getDiscordId()),
              Optional.of(linkedProfile.getRole()),
              Optional.of(linkedProfile.getLockedUntil())
            );
        }

        return new LinkedPlayerDto(
          Optional.of(nameAndId.id()),
          Optional.of(nameAndId.name()),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        );
    }

    public boolean isLinked() {
        return discordId().isPresent() && role().isPresent();
    }

    public Optional<String> discordId() {
        return discordId;
    }

    public Optional<String> role() {
        return role;
    }

    public Optional<Long> lockedUntil() {
        return lockedUntil;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            if (!(boolean) EQUALS.invoke(this, obj)) return false;
        } catch (Error error) {
            throw error; // We don't want to catch unrecoverable errors
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


        if (obj instanceof LinkedPlayerDto dto) {
            return dto.discordId.equals(this.discordId) && dto.role.equals(this.role) && dto.lockedUntil.equals(this.lockedUntil);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), name(), discordId(), role());
    }

    @Override
    public @NotNull String toString() {
        return new ToStringBuilder(this)
          .append("id", id())
          .append("name", name())
          .append("discordId", discordId())
          .append("role", role())
          .append("lockedUntil", lockedUntil())
          .toString();
    }

    static {
        try {
            EQUALS = MethodHandles.lookup().findSpecial(PlayerDto.class, "equals", MethodType.methodType(boolean.class, Object.class), LinkedPlayerDto.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Failed to get known method handle!");
        }
    }
}
