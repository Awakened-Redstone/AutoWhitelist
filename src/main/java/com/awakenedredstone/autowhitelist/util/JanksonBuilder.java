package com.awakenedredstone.autowhitelist.util;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.DeserializerFunction;
import com.awakenedredstone.autowhitelist.config.source.annotation.PredicateConstraint;
import com.awakenedredstone.autowhitelist.config.source.annotation.RangeConstraint;
import com.awakenedredstone.autowhitelist.config.source.annotation.RegexConstraint;
import com.awakenedredstone.autowhitelist.config.source.jankson.Marshaller;
import com.awakenedredstone.autowhitelist.mixin.compat.JanksonAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JanksonBuilder {
    public static final Logger LOGGER = LoggerFactory.getLogger("Jankson Marshaller");
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
    public static Jankson buildJankson(Consumer<Builder> janksonBuilder) {
        Builder builder = new Builder()
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
          .registerSerializer(Vec3d.class, (vec3d, marshaller) -> {
              JsonArray array = new JsonArray();
              array.add(new JsonPrimitive(vec3d.getX()));
              array.add(new JsonPrimitive(vec3d.getY()));
              array.add(new JsonPrimitive(vec3d.getZ()));
              return array;
          })
          .registerDeserializer(JsonArray.class, Vec3d.class, (json, m) -> new Vec3d(json.getDouble(0, 0), json.getDouble(1, 0), json.getDouble(2, 0)))
          //Annotation
          .registerAnnotationProcessor(PredicateConstraint.class, (annotation, object, clazz, fieldName) -> {
              List<Method> methods = Arrays.stream(clazz.getMethods()).filter(method -> method.getName().equals(annotation.value())).toList();
              Method method;
              if (methods.size() > 1) {
                  LOGGER.warn("Multiple methods found with the same name, trying to find the most suitable one");
                  method = methods.stream().filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(object.getClass())).findFirst().orElse(null);
                  if (method == null) {
                      throw new IllegalStateException("No suitable " + annotation.value() + " method found for " + fieldName);
                  }
              } else if (methods.size() == 1) {
                  method = methods.get(0);
              } else {
                  throw new IllegalStateException("No " + annotation.value() + " method found for " + fieldName);
              }

              if (method.getReturnType() != boolean.class) {
                  throw new IllegalStateException("The method " + method.getName() + " must return a " + boolean.class.getSimpleName());
              } else if (method.getParameterCount() != 1) {
                  throw new IllegalStateException("The method " + method.getName() + " must have only one parameter");
              } else if (!method.getParameterTypes()[0].isAssignableFrom(object.getClass())) {
                  throw new IllegalStateException("The method " + method.getName() + " must have a parameter of type " + object.getClass().getSimpleName());
              }

              try {
                  return (boolean) method.invoke(null, object);
              } catch (Throwable e) {
                  throw new IllegalStateException("Failed to invoke " + method.getName() + " for " + fieldName, e);
              }
          })
          .registerAnnotationProcessor(RegexConstraint.class, (annotation, object, clazz, fieldName) -> object.toString().matches(annotation.value()))
          .registerAnnotationProcessor(RangeConstraint.class, (annotation, object, clazz, fieldName) -> {
              if (object instanceof Number number) {
                  boolean minValid = annotation.minInclusive() ? number.doubleValue() >= annotation.min() : number.doubleValue() > annotation.min();
                  boolean maxValid = annotation.maxInclusive() ? number.doubleValue() <= annotation.max() : number.doubleValue() < annotation.max();
                  boolean minusOneValid = !annotation.minusOne() || number.doubleValue() != -1;
                  return minValid && maxValid && minusOneValid;
              } else {
                  throw new IllegalStateException("The field " + fieldName + " must be a number to use the " + annotation.annotationType().getSimpleName());
              }
          });

        janksonBuilder.accept(builder);
        return builder.build();
    }

    static {
        JANKSON = buildJankson();
    }

    public static class Builder extends Jankson.Builder {
        protected Marshaller marshaller = new Marshaller();
        boolean allowBareRootObject = false;

        /**
         * Registers a function to serialize an object into json. This can be useful if a class's serialized form is not
         * meant to resemble its live-memory form.
         *
         * @param clazz      The class to register a serializer for
         * @param serializer A function which takes the object and a Marshaller, and produces a serialized JsonElement
         * @return This Builder for further modificaton.
         */
        public <T> Builder registerSerializer(Class<T> clazz, BiFunction<T, blue.endless.jankson.api.Marshaller, JsonElement> serializer) {
            marshaller.registerSerializer(clazz, serializer);
            return this;
        }

        public <A, B> Builder registerDeserializer(Class<A> sourceClass, Class<B> targetClass, DeserializerFunction<A, B> function) {
            marshaller.registerDeserializer(sourceClass, targetClass, function);
            return this;
        }

        public <A extends Annotation, O> Builder registerAnnotationProcessor(Class<A> annotation, Marshaller.AnnotationProcessor<A, O> processor) {
            marshaller.registerAnnotationProcessor(annotation, processor);
            return this;
        }

        /**
         * Registers a factory that can generate empty objects of the specified type. Sometimes it's not practical
         * to have a no-arg constructor available on an object, so the function to create blanks can be specified
         * here.
         *
         * @param clazz   The class to use an alternate factory for
         * @param factory A Supplier which can create blank objects of class `clazz` for deserialization
         * @return This Builder for further modification.
         */
        public <T> Builder registerTypeFactory(Class<T> clazz, Supplier<T> factory) {
            marshaller.registerTypeFactory(clazz, factory);
            return this;
        }

        /**
         * Allows loading JSON files that do not contain root braces, as generated with
         * {@link JsonGrammar.Builder#bareRootObject(boolean) bareRootObject}.
         *
         * @return This Builder for further modification.
         */
        public Builder allowBareRootObject() {
            allowBareRootObject = true;
            return this;
        }

        public Jankson build() {
            Jankson result = JanksonAccessor.createJankson(this);
            JanksonAccessor accessor = (JanksonAccessor) result;
            accessor.setMarshaller(marshaller);
            accessor.setAllowBareRootObject(allowBareRootObject);
            return result;
        }
    }
}
