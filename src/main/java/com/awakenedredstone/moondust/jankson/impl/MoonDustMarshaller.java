package com.awakenedredstone.moondust.jankson.impl;

import com.awakenedredstone.moondust.jankson.annotation.*;
import com.awakenedredstone.moondust.jankson.element.*;
import com.awakenedredstone.moondust.jankson.element.primitive.*;
import com.awakenedredstone.moondust.jankson.api.DeserializationException;
import com.awakenedredstone.moondust.jankson.api.DeserializerFunction;
import com.awakenedredstone.moondust.jankson.api.Marshaller;
import com.awakenedredstone.moondust.jankson.impl.serializer.DeserializerFunctionPool;
import com.awakenedredstone.moondust.jankson.magic.TypeMagic;
import com.awakenedredstone.moondust.config.api.CodecSpecs;
import com.awakenedredstone.moondust.config.api.exception.SerializationException;
import com.awakenedredstone.autowhitelist.entry.api.serialization.JanksonOps;
import com.google.common.base.CaseFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MoonDustMarshaller implements Marshaller {
    public static final Logger LOGGER = LoggerFactory.getLogger(MoonDustMarshaller.class);
    private static final MoonDustMarshaller INSTANCE = new MoonDustMarshaller();

    public static MoonDustMarshaller getFallback() {
        return INSTANCE;
    }

    private final Map<Class<?>, Function<Object, ?>> primitiveMarshallers = new HashMap<>();

    private final Map<Class<?>, Codec<?>> codecs = new HashMap<>();

    private final Map<Class<?>, BiFunction<Object, Marshaller, JsonElement>> serializers = new HashMap<>();
    private final Map<Class<?>, DeserializerFunctionPool<?>> deserializers = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> typeFactories = new HashMap<>();

    public <T> void register(Class<T> clazz, Function<Object, T> marshaller) {
        primitiveMarshallers.put(clazz, marshaller);
    }

    public <T> void register(Class<T> primitiveClass, Class<T> wrapperClass, Function<Object, T> marshaller) {
        primitiveMarshallers.put(primitiveClass, marshaller);
        primitiveMarshallers.put(wrapperClass, marshaller);
    }

    public <T> void registerCodec(Class<T> clazz, Codec<T> codec) {
        codecs.put(clazz, codec);
    }

    @SuppressWarnings("unchecked")
    public <T> void registerSerializer(Class<T> clazz, Function<T, JsonElement> serializer) {
        serializers.put(clazz, (it, marshaller) -> serializer.apply((T) it));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerSerializer(Class<T> primitiveClass, Class<T> wrapperClass, Function<T, JsonElement> serializer) {
        serializers.put(primitiveClass, (it, marshaller) -> serializer.apply((T) it));
        serializers.put(wrapperClass, (it, marshaller) -> serializer.apply((T) it));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerSerializer(Class<T> clazz, BiFunction<T, Marshaller, JsonElement> serializer) {
        serializers.put(clazz, (BiFunction<Object, Marshaller, JsonElement>) serializer);
    }

    public <T> void registerTypeFactory(Class<T> clazz, Supplier<T> supplier) {
        typeFactories.put(clazz, supplier);
    }

    public <A, B> void registerDeserializer(Class<A> sourceClass, Class<B> targetClass, DeserializerFunction<A, B> function) {
        @SuppressWarnings("unchecked")
        DeserializerFunctionPool<B> pool = (DeserializerFunctionPool<B>) deserializers.get(targetClass);
        if (pool == null) {
            pool = new DeserializerFunctionPool<>(targetClass);
            deserializers.put(targetClass, pool);
        }
        pool.registerUnsafe(sourceClass, function);
    }

    public MoonDustMarshaller() {
        // Primitives
        register(String.class, Object::toString);
        register(void.class, Void.class, (it) -> null);
        register(boolean.class, Boolean.class, (it) -> (it instanceof Boolean) ? (Boolean) it : null);
        register(char.class, Character.class, (it) -> (it instanceof Number) ? (char) ((Number) it).shortValue() : it.toString().charAt(0));
        register(byte.class, Byte.class, (it) -> (it instanceof Number) ? ((Number) it).byteValue() : null);
        register(short.class, Short.class, (it) -> (it instanceof Number) ? ((Number) it).shortValue() : null);
        register(int.class, Integer.class, (it) -> (it instanceof Number) ? ((Number) it).intValue() : null);
        register(long.class, Long.class, (it) -> (it instanceof Number) ? ((Number) it).longValue() : null);
        register(float.class, Float.class, (it) -> (it instanceof Number) ? ((Number) it).floatValue() : null);
        register(double.class, Double.class, (it) -> (it instanceof Number) ? ((Number) it).doubleValue() : null);

        registerSerializer(String.class, JsonText::new);
        registerSerializer(void.class, Void.class, (it) -> JsonNull.INSTANCE);
        registerSerializer(boolean.class, Boolean.class, JsonBoolean::new);
        registerSerializer(char.class, Character.class, JsonCharacter::new);
        registerSerializer(byte.class, Byte.class, JsonByte::new);
        registerSerializer(short.class, Short.class, JsonShort::new);
        registerSerializer(int.class, Integer.class, JsonInteger::new);
        registerSerializer(long.class, Long.class, JsonLong::new);
        registerSerializer(float.class, Float.class, JsonFloat::new);
        registerSerializer(double.class, Double.class, JsonDouble::new);

        // Common Java classes
        registerCodec(UUID.class, UUIDUtil.STRING_CODEC);

        // Minecraft classes
        registerCodec(Identifier.class, Identifier.CODEC);
        registerCodec(Vec3.class, Vec3.CODEC);
        registerCodec(BlockPos.class, BlockPos.CODEC);
    }

    /**
     * EXPERIMENTAL. Marshalls elem into a very specific parameterized type, honoring generic type arguments.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T marshall(Type type, JsonElement elem) {
        if (elem == null) return null;
        if (elem == JsonNull.INSTANCE) return null;

        if (type instanceof Class) {
            try {
                return marshall((Class<T>) type, elem);
            } catch (ClassCastException t) {
                return null;
            }
        }

        if (type instanceof ParameterizedType) {
            try {
                Class<T> clazz = (Class<T>) TypeMagic.classForType(type);

                return marshall(clazz, elem);
            } catch (ClassCastException t) {
                return null;
            }
        }

        return null;
    }

    public <T> T marshall(Class<T> clazz, JsonElement elem) {
        try {
            return marshall(clazz, elem, false);
        } catch (Exception t) {
            LOGGER.debug("Failed to parse JSON into class {}", clazz.getCanonicalName(), t);
            return null;
        }
    }

    public <T> T marshallCarefully(Class<T> clazz, JsonElement elem) throws DeserializationException {
        return marshall(clazz, elem, true);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T marshall(Class<T> clazz, JsonElement elem, boolean strict) throws DeserializationException {
        if (elem == null) return null;
        if (elem == JsonNull.INSTANCE) return null;
        if (clazz.isAssignableFrom(elem.getClass())) return (T) elem; // Already the correct type

        Codec<T> codec = (Codec<T>) codecs.get(clazz);
        if (codec != null) {
            DataResult<T> dataResult = codec.parse(JanksonOps.INSTANCE, elem);
            if (strict) {
                return dataResult.getOrThrow(DeserializationException::new);
            }

            if (dataResult.error().isPresent()) {
                LOGGER.debug("Failed to parse Codec! {}", dataResult.error().get().message());
            }

            return dataResult.result().orElse(null);
        }

        // Externally registered deserializers
        DeserializerFunctionPool<T> pool = (DeserializerFunctionPool<T>) deserializers.get(clazz);
        if (pool != null) {
            try {
                return pool.apply(elem, this);
            } catch (DeserializerFunctionPool.FunctionMatchFailedException e) {
                // Don't return the result, but continue
            }
        }

        //Internally annotated deserializers
        pool = POJODeserializer.deserializersFor(clazz);
        T poolResult;
        try {
            poolResult = pool.apply(elem, this);
            return poolResult;
        } catch (DeserializerFunctionPool.FunctionMatchFailedException e) {
            // Don't return the result, but continue
        }


        if (Enum.class.isAssignableFrom(clazz)) {
            if (!(elem instanceof JsonPrimitive<?> primitive)) return null;
            String name = primitive.asString();

            T[] constants = clazz.getEnumConstants();
            if (constants == null) return null;
            for (T constant : constants) {
                if (((Enum<?>) constant).name().equals(name)) return constant;
                if (((Enum<?>) constant).name().equals(name.toUpperCase())) return constant;
            }
        }

        if (clazz.equals(String.class)) {
            //Almost everything has a String representation
            switch (elem) {
                case JsonObject ignored -> {
                    return (T) elem.toJson(false, false);
                }
                case JsonArray ignored -> {
                    return (T) elem.toJson(false, false);
                }
                case JsonPrimitive<?> primitive -> {
                    return (T) primitive.asString();
                }
                case JsonNull ignored -> {
                    return (T) "null";
                }
                default -> {
                }
            }

            if (strict) {
                throw new DeserializationException("Encountered unexpected JsonElement type while deserializing to string: " + elem.getClass().getCanonicalName());
            }

            LOGGER.debug("Encountered unexpected JsonElement type while deserializing to string: {}", elem.getClass().getCanonicalName());
            return null;
        }

        switch (elem) {
            case JsonPrimitive<?> jsonPrimitive -> {
                Function<Object, ?> func = primitiveMarshallers.get(clazz);
                if (func != null) {
                    return (T) func.apply(jsonPrimitive.value());
                } else {
                    if (strict) {
                        throw new DeserializationException("Don't know how to unpack value '" + elem + "' into target type '" + clazz.getCanonicalName() + "'");
                    }
                    LOGGER.debug("Don't know how to unpack value '{}' into target type '{}'", elem, clazz.getCanonicalName());
                    return null;
                }
            }
            case JsonObject obj -> {
                if (clazz.isPrimitive())
                    throw new DeserializationException("Can't marshall json object into primitive type " + clazz.getCanonicalName());
                if (JsonPrimitive.class.isAssignableFrom(clazz)) {
                    if (strict) throw new DeserializationException("Can't marshall json object into a json primitive");
                    LOGGER.debug("Can't marshall json object into a json primitive");
                    return null;
                }

                obj.setMarshaller(this);

                if (typeFactories.containsKey(clazz)) {
                    T result = (T) typeFactories.get(clazz).get();
                    try {
                        POJODeserializer.unpackObject(result, obj, strict);
                        return result;
                    } catch (Exception t) {
                        if (strict) throw t;
                        LOGGER.debug("Failed to parse JSON into class {} [typeFactories]", clazz.getCanonicalName(), t);
                        return null;
                    }
                } else {

                    try {
                        T result = Objects.requireNonNull(TypeMagic.createAndCast(clazz, strict));
                        POJODeserializer.unpackObject(result, obj, strict);

                        return result;
                    } catch (Exception e) {
                        if (strict) throw e;
                        LOGGER.debug("Failed to parse JSON into class {} [typeFactories$else]", clazz.getCanonicalName(), e);
                        return null;
                    }
                }
            }
            case JsonArray array -> {
                if (clazz.isPrimitive()) {
                    LOGGER.debug("Failed to parse JSON into class {} [elem instanceof JsonArray]", clazz.getCanonicalName());
                    return null;
                }
                if (clazz.isArray()) {
                    Class<?> componentType = clazz.getComponentType();

                    T result = (T) Array.newInstance(componentType, array.size());
                    for (int i = 0; i < array.size(); i++) {
                        Array.set(result, i, marshall(componentType, array.get(i)));
                    }
                    return result;
                }
            }
            default -> {
            }
        }

        LOGGER.debug("Failed to parse JSON into class {} [END]", clazz.getCanonicalName());
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> DataResult<T> marshallFieldCodec(String name, CodecSpecs specs, JsonElement element) {
        Codec<T> codec = (Codec<T>) specs.getCodecs().get(name);
        if (codec == null) {
            return null;
        }

        return codec.parse(JanksonOps.INSTANCE, element);
    }

    @SuppressWarnings("unchecked")
    public JsonElement serialize(Object obj) {
        if (obj == null) return JsonNull.INSTANCE;

        //Prefer exact match
        Codec<Object> codec = (Codec<Object>) codecs.get(obj.getClass());
        if (codec != null) {
            return codec.encodeStart(JanksonOps.INSTANCE, obj).getOrThrow(SerializationException::new);
        }

        BiFunction<Object, Marshaller, JsonElement> serializer = serializers.get(obj.getClass());
        if (serializer != null) {
            JsonElement result = serializer.apply(obj, this);
            if (result instanceof JsonObject) ((JsonObject) result).setMarshaller(this);
            if (result instanceof JsonArray) ((JsonArray) result).setMarshaller(this);
            return result;
        } else {
            //Detailed match
            for (Map.Entry<Class<?>, BiFunction<Object, Marshaller, JsonElement>> entry : serializers.entrySet()) {
                if (entry.getKey().isAssignableFrom(obj.getClass())) {
                    JsonElement result = entry.getValue().apply(obj, this);
                    if (result instanceof JsonObject) ((JsonObject) result).setMarshaller(this);
                    if (result instanceof JsonArray) ((JsonArray) result).setMarshaller(this);
                    return result;
                }
            }
        }

        //Check for annotations
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(Serializer.class) && !Modifier.isStatic(m.getModifiers())) {
                Class<?> clazz = m.getReturnType();
                if (JsonElement.class.isAssignableFrom(clazz)) {
                    //This is probably the method we're looking for! Let's figure out its method signature!
                    Parameter[] params = m.getParameters();
                    if (params.length == 0) {
                        try {
                            boolean access = m.canAccess(obj);
                            if (!access) m.setAccessible(true);
                            JsonElement result = (JsonElement) m.invoke(obj);
                            if (!access) m.setAccessible(false);
                            if (result instanceof JsonObject) ((JsonObject) result).setMarshaller(this);
                            if (result instanceof JsonArray) ((JsonArray) result).setMarshaller(this);
                            return result;
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new SerializationException("Failed to serialize value %s of class %s (params length 0)".formatted(obj, obj.getClass().getCanonicalName()), e);
                        }
                    } else if (params.length == 1) {
                        if (Marshaller.class.isAssignableFrom(params[0].getType())) {
                            try {
                                boolean access = m.canAccess(obj);
                                if (!access) m.setAccessible(true);
                                JsonElement result = (JsonElement) m.invoke(obj, this);
                                if (!access) m.setAccessible(false);
                                if (result instanceof JsonObject) ((JsonObject) result).setMarshaller(this);
                                if (result instanceof JsonArray) ((JsonArray) result).setMarshaller(this);
                                return result;
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                                throw new SerializationException("Failed to serialize value %s of class %s (params length 1)".formatted(obj, obj.getClass().getCanonicalName()), e);
                            }
                        }
                    }
                }
            }
        }

        if (obj instanceof Enum) {
            return new JsonText(((Enum<?>) obj).name());
        }

        if (obj.getClass().isArray()) {
            JsonArray array = new JsonArray();
            array.setMarshaller(this);
            //Class<?> component = obj.getClass().getComponentType();
            for (int i = 0; i < Array.getLength(obj); i++) {
                Object elem = Array.get(obj, i);
                JsonElement parsed = serialize(elem);
                array.add(parsed);
            }
            return array;
        }

        if (obj instanceof Collection) {
            JsonArray array = new JsonArray();
            array.setMarshaller(this);
            for (Object elem : (Collection<?>) obj) {
                JsonElement parsed = serialize(elem);
                array.add(parsed);
            }
            return array;
        }

        if (obj instanceof Map) {
            JsonObject result = new JsonObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                String k = entry.getKey().toString();
                Object v = entry.getValue();
                result.put(k, serialize(v));
            }
            return result;
        }

        NameFormat defaultNameFormat = obj.getClass().getAnnotation(NameFormat.class);

        JsonObject result = new JsonObject();
        //Pull in public fields first
        for (Field field : obj.getClass().getFields()) {
            if (
              Modifier.isStatic(field.getModifiers()) || // Not part of the object
              Modifier.isTransient(field.getModifiers()) //Never serialize
            ) continue;

            processField(obj, defaultNameFormat, result, field);
        }

        //Add in what private fields we can reach
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (
              Modifier.isPublic(field.getModifiers()) || // Already serialized
              Modifier.isStatic(field.getModifiers()) || // Not part of the object
              Modifier.isTransient(field.getModifiers()) //Never serialize
            ) continue;

            processField(obj, defaultNameFormat, result, field);
        }

        return result;
    }

    private void processField(Object obj, NameFormat defaultNameFormat, JsonObject result, Field field) {
        // Make sure it is accessible
        field.setAccessible(true);

        try {
            Object child = field.get(obj);
            String name = getFieldName(defaultNameFormat, field);

            Comment comment = field.getAnnotation(Comment.class);
            Secret secret = field.getAnnotation(Secret.class);

            InnerEntry.Meta meta = new InnerEntry.Meta();

            if (comment != null) {
                String commentText = comment.value();
                if (comment.stripIndent()) {
                    if (comment.stripWhitespace()) commentText = commentText.stripTrailing();
                    commentText = commentText.stripIndent();
                }

                if (comment.stripWhitespace()) commentText = commentText.strip();

                meta.setComment(commentText);
            }

            if (secret != null) meta.setSecret(secret.value());

            JsonElement serialized = null;
            if (obj instanceof CodecSpecs specs) {
                var dataResult = processFieldCodec(name, specs, child);
                if (dataResult != null) {
                    serialized = dataResult.getOrThrow(SerializationException::new);
                }
            }

            if (serialized == null) {
                serialized = serialize(child);
            }

            result.put(name, serialized, meta);
        } catch (IllegalArgumentException | IllegalAccessException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public <T> DataResult<JsonElement> processFieldCodec(String name, CodecSpecs specs, T element) {
        Codec<T> codec = (Codec<T>) specs.getCodecs().get(name);
        if (codec == null) {
            return null;
        }

        return codec.encodeStart(JanksonOps.INSTANCE, element);
    }

    private static String getFieldName(NameFormat defaultNameFormat, Field field) {
        // If a serialized name is set it is used over the other formatters
        SerializedName serializedName = field.getAnnotation(SerializedName.class);
        if (serializedName != null) {
            return serializedName.value();
        }

        // If the name format is skipped then the name is the field name in the class
        SkipNameFormat skipNameFormat = field.getAnnotation(SkipNameFormat.class);
        if (skipNameFormat != null) return field.getName();

        // Check if the field has a format set for it's name
        NameFormat nameFormat = field.getAnnotation(NameFormat.class);
        if (nameFormat != null || defaultNameFormat != null) {
            // If no format is set for the field, use the default one
            NameFormat formatter = nameFormat != null ? nameFormat : defaultNameFormat;
            // Java fields are expected to be in lower camelcase
            return CaseFormat.LOWER_CAMEL.to(formatter.value().getCaseFormat(), field.getName());
        }

        // If no formatting was set, then use the field name in the class
        return field.getName();
    }
}
