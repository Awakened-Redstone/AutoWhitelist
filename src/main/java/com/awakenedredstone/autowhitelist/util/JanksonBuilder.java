package com.awakenedredstone.autowhitelist.util;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonPrimitive;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

public class JanksonBuilder {
    public static final Logger LOGGER = LoggerFactory.getLogger("CherryBlossomConfigAPI");
    public static final Jankson JANKSON;

    /**
     * Creates a Jankson instance with the default serializers and deserializers, it takes a consumer as parameter
     * that can be used to add custom serializers and deserializers.
     *
     * @return A Jankson instance with the default and provided custom serializers and deserializers
     */
    public static Jankson buildJankson() {
        return buildJankson(builder -> {
        });
    }

    /**
     * Creates a Jankson instance with the default serializers and deserializers, it takes a consumer as parameter
     * that can be used to add custom serializers and deserializers.
     *
     * @param janksonBuilder A consumer that can be used to add custom serializers and deserializers.
     * @return A Jankson instance with the default and provided custom serializers and deserializers
     */
    public static Jankson buildJankson(Consumer<Jankson.Builder> janksonBuilder) {
        Jankson.Builder builder = Jankson.builder()
            //Identifier
            .registerSerializer(Identifier.class, (identifier, marshaller) -> new JsonPrimitive(identifier.toString()))
            .registerDeserializer(JsonPrimitive.class, Identifier.class, (primitive, m) -> Identifier.tryParse(primitive.asString()))

            //UUID
            .registerSerializer(UUID.class, (uuid, marshaller) -> {
                JsonArray array = new JsonArray();
                array.add(new JsonPrimitive(uuid.getMostSignificantBits()));
                array.add(new JsonPrimitive(uuid.getLeastSignificantBits()));
                return array;
            })
            .registerDeserializer(JsonArray.class, UUID.class, (json, m) -> new UUID(json.getLong(0, 0), json.getLong(1, 0)))

            //BlockPos
            .registerSerializer(BlockPos.class, (blockPos, marshaller) -> {
                JsonArray array = new JsonArray();
                array.add(new JsonPrimitive(blockPos.getX()));
                array.add(new JsonPrimitive(blockPos.getY()));
                array.add(new JsonPrimitive(blockPos.getZ()));
                return array;
            })
            .registerDeserializer(JsonArray.class, BlockPos.class, (json, m) -> new BlockPos(json.getInt(0, 0), json.getInt(1, 0), json.getInt(2, 0)))

            //Vec3d
            .registerSerializer(Vec3d.class, (blockPos, marshaller) -> {
                JsonArray array = new JsonArray();
                array.add(new JsonPrimitive(blockPos.getX()));
                array.add(new JsonPrimitive(blockPos.getY()));
                array.add(new JsonPrimitive(blockPos.getZ()));
                return array;
            })
            .registerDeserializer(JsonArray.class, Vec3d.class, (json, m) -> new Vec3d(json.getDouble(0, 0), json.getDouble(1, 0), json.getDouble(2, 0)));

        janksonBuilder.accept(builder);
        return builder.build();
    }

    static {
        JANKSON = buildJankson();
    }
}
