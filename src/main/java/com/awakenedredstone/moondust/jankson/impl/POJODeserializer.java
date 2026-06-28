/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.awakenedredstone.moondust.jankson.impl;

import com.awakenedredstone.moondust.config.api.CodecSpecs;
import com.awakenedredstone.moondust.jankson.element.JsonArray;
import com.awakenedredstone.moondust.jankson.element.JsonElement;
import com.awakenedredstone.moondust.jankson.JsonGrammar;
import com.awakenedredstone.moondust.jankson.element.JsonNull;
import com.awakenedredstone.moondust.jankson.element.JsonObject;
import com.awakenedredstone.moondust.jankson.annotation.Deserializer;
import com.awakenedredstone.moondust.jankson.annotation.NameFormat;
import com.awakenedredstone.moondust.jankson.annotation.SerializedName;
import com.awakenedredstone.moondust.jankson.annotation.SkipNameFormat;
import com.awakenedredstone.moondust.jankson.api.DeserializationException;
import com.awakenedredstone.moondust.jankson.api.Marshaller;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonText;
import com.awakenedredstone.moondust.jankson.impl.serializer.InternalDeserializerFunction;
import com.awakenedredstone.moondust.jankson.impl.serializer.DeserializerFunctionPool;
import com.awakenedredstone.moondust.jankson.magic.TypeMagic;
import com.google.common.base.CaseFormat;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;

public class POJODeserializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(POJODeserializer.class);
    public static void unpackObject(Object target, JsonObject source) {
        try {
            unpackObject(target, source, false);
        } catch (Exception ignored) {
        }
    }

    public static void unpackObject(Object target, JsonObject source, boolean strict) throws DeserializationException {
        //if (o.getClass().getTypeParameters().length>0) throw new DeserializationException("Can't safely deserialize generic types!");
        //well, let's try anyway and see if we run into problems.

        //Create a copy we can redact keys from
        JsonObject work = source.clone();

        //Fill public and private fields declared in the target object's immediate class
        for (Field field : target.getClass().getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
            unpackField(target, field, work, strict);
        }

        //Attempt to fill public, accessible fields declared in the target object's superclass.
        for (Field field : target.getClass().getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
            unpackField(target, field, work, strict);
        }

        if (!work.isEmpty() && strict) {
            throw new DeserializationException("There was data that couldn't be applied to the destination object: " + work.toJson(JsonGrammar.STRICT));
        }
    }

    public static void unpackField(Object parent, Field field, JsonObject source, boolean strict) throws DeserializationException {
        String fieldName = fieldName(parent, field);

        if (source.containsKey(fieldName)) {
            JsonElement elem = source.get(fieldName);
            source.remove(fieldName); // Prevent it from getting re-unpacked
            if (elem == null || elem == JsonNull.INSTANCE) {
                boolean accessible = field.canAccess(parent);
                if (!accessible) field.setAccessible(true);
                try {
                    field.set(parent, null);
                    if (!accessible) field.setAccessible(false);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    if (strict) {
                        throw new DeserializationException("Couldn't set field \"" + field.getName() + "\" of class \"" + parent.getClass().getCanonicalName() + "\"", e);
                    }
                    LOGGER.warn("Failed to unpack field", e);
                }
            } else {
                try {
                    unpackFieldData(parent, field, elem, source.getMarshaller());
                } catch (Exception e) {
                    if (strict) {
                        throw new DeserializationException("There was a problem unpacking field " + field.getName() + " of class " + parent.getClass().getCanonicalName(), e);
                    }
                    LOGGER.warn("Failed to unpack field", e);
                }
            }
        }
    }

    private static String fieldName(Object parent, Field field) {
        String fieldName = field.getName();

        NameFormat defaultNameFormat = parent.getClass().getAnnotation(NameFormat.class);

        NameFormat nameFormat = field.getAnnotation(NameFormat.class);
        SkipNameFormat skipNameFormat = field.getAnnotation(SkipNameFormat.class);
        SerializedName nameAnnotation = field.getAnnotation(SerializedName.class);
        if (skipNameFormat == null && (nameFormat != null || defaultNameFormat != null) && nameAnnotation == null) {
            NameFormat formatter = nameFormat != null ? nameFormat : defaultNameFormat;
            return CaseFormat.LOWER_CAMEL.to(formatter.value().getCaseFormat(), fieldName);
        }

        return fieldName;
    }

    /**
     * NOT WORKING YET, HIGHLY EXPERIMENTAL
     */
    @Nullable
    public static Object unpack(Type t, JsonElement elem, Marshaller marshaller) {
        Class<?> rawClass = TypeMagic.classForType(t);
        if (rawClass.isPrimitive()) return null; //We can't unpack a primitive into an object of primitive type. Maybe in the future we can return a boxed type?

        if (elem == null) return null;
		/*
		if (type instanceof Class) {
			try {
				return marshaller.marshall((Class<?>) type, elem);
			} catch (ClassCastException t) {
				return null;
			}
		}
		
		if (type instanceof ParameterizedType) {
			try {
				Class<?> clazz = (Class<?>) TypeMagic.classForType(type);
				
				if (List.class.isAssignableFrom(clazz)) {
					Object result = TypeMagic.createAndCast(type);
					
					try {
						unpackList((List<Object>) result, type, elem, marshaller);
						return result;
					} catch (DeserializationException e) {
						e.printStackTrace();
						return result;
					}
				}
				
				return null;
			} catch (ClassCastException t) {
				return null;
			}
		}*/

        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean unpackFieldData(Object parent, Field field, JsonElement elem, Marshaller marshaller) throws Exception {
        if (elem == null) return true;
        try {
            field.setAccessible(true);
        } catch (InaccessibleObjectException t) {
            return false; //skip this field, probably.
        }

        if (elem == JsonNull.INSTANCE) {
            field.set(parent, null);
            return true;
        }

        Class<?> fieldClass = field.getType();

        // TODO: abstract codec support
        if (marshaller instanceof MoonDustMarshaller moondust && parent instanceof CodecSpecs specs) {
            String name = fieldName(parent, field);
            DataResult<Object> marshalResult = moondust.marshallFieldCodec(name, specs, elem);

            if (marshalResult != null) {
                Object result = marshalResult.getOrThrow(DeserializationException::new);
                field.set(parent, result);
                return true;
            }
        }

        if (!isCollections(fieldClass)) {
            // Try to directly marshall
            Object result = marshaller.marshallCarefully(fieldClass, elem);
            field.set(parent, result);
            return true;
        }


        if (field.get(parent) == null) {
            Object fieldValue = TypeMagic.createAndCast(field.getGenericType());

            if (fieldValue == null) {
                return false; //Can't deserialize this somehow
            } else {
                field.set(parent, fieldValue);
            }
        }

        if (Map.class.isAssignableFrom(fieldClass)) {
            Type[] parameters = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();

            unpackMap((Map<Object, Object>) field.get(parent), parameters[0], parameters[1], elem, marshaller);

            return true;
        }

        if (Collection.class.isAssignableFrom(fieldClass)) {
            Type elementType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

            unpackCollection((Collection<Object>) field.get(parent), elementType, elem, marshaller);

            return true;
        }

        return false;
    }

    private static boolean isCollections(Class<?> clazz) {
        return
          Map.class.isAssignableFrom(clazz) ||
          Collection.class.isAssignableFrom(clazz);
    }

    public static void unpackMap(Map<Object, Object> map, Type keyType, Type valueType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
        if (!(elem instanceof JsonObject object)) {
            throw new DeserializationException("Cannot deserialize a " + elem.getClass().getSimpleName() + " into a Map - expected a JsonObject!");
        }

        map.clear(); //This may be a user-supplied collection, initialized in the constructor with default mappings. Erase those.

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                Object k = marshaller.marshall(keyType, new JsonText(entry.getKey()));
                Object v = marshaller.marshall(valueType, entry.getValue());
                if (k != null && v != null) map.put(k, v);
            } catch (Exception ignored) {}
        }
    }

    public static void unpackCollection(Collection<Object> collection, Type elementType, JsonElement elem, Marshaller marshaller) throws DeserializationException {
        if (!(elem instanceof JsonArray array)) {
            throw new DeserializationException("Cannot deserialize a " + elem.getClass().getSimpleName() + " into a Set - expected a JsonArray!");
        }

        collection.clear(); //This may be a user-supplied collection, initialized in the constructor with default items. Erase those.

        for (JsonElement arrayElem : array) {
            Object object = marshaller.marshall(elementType, arrayElem);
            if (object != null) collection.add(object);
        }
    }

    protected static <B> DeserializerFunctionPool<B> deserializersFor(Class<B> targetClass) {
        DeserializerFunctionPool<B> pool = new DeserializerFunctionPool<>(targetClass);
        for (Method m : targetClass.getDeclaredMethods()) {
            //System.out.println("Examining "+m.getName());
            if (m.getAnnotation(Deserializer.class) == null) continue; //Must be annotated

            if (!Modifier.isStatic(m.getModifiers())) continue; //Must be static
            if (!m.getReturnType().equals(targetClass)) continue; //Must return an instance of the class
            //System.out.println("    Cleared first screening");
            Parameter[] params = m.getParameters();
            if (params.length >= 1) {
                Class<?> sourceClass = params[0].getType();
                InternalDeserializerFunction<B> deserializer = makeDeserializer(m, sourceClass, targetClass);
                if (deserializer == null) continue;
                pool.registerUnsafe(sourceClass, deserializer);
                //System.out.println("    Registered deserializer");
            }
        }
        return pool;
    }

    /**
     * Assuming the method is a valid deserializer, and matches the type signature required, produces a DeserializerFunction which delegates to the method provided.
     * If the method is not a valid deserializer of this type, returns null instead.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static <A, B> InternalDeserializerFunction<B> makeDeserializer(@NotNull Method m, @NotNull Class<A> sourceClass, @NotNull Class<B> targetClass) {
        if (!m.getReturnType().equals(targetClass)) return null;
        Parameter[] params = m.getParameters();
        if (params.length == 1) {
            //if (params[0].getClass().isAssignableFrom(sourceClass)) {
            return (Object o, Marshaller marshaller) -> {
                try {
                    return (B) m.invoke(null, o);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new DeserializationException(ex);
                }
            };
            //}
            //return null;
        } else if (params.length == 2) {
            //if (params[0].getClass().isAssignableFrom(sourceClass)) {
            if (params[1].getType().equals(Marshaller.class)) {
                return (Object o, Marshaller marshaller) -> {
                    try {
                        return (B) m.invoke(null, o, marshaller);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new DeserializationException(ex);
                    }
                };
            }
            //}
            return null;
        } else {
            return null;
        }
    }
}
