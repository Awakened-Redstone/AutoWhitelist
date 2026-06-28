package com.awakenedredstone.autowhitelist.discord.message;

import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.rest.util.AllowedMentions;
import net.minecraft.resources.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseMessage {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final Map<Identifier, MethodHandle> BUILDERS = new HashMap<>();

    public static <T> Identifier register(Identifier id, final T builder) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        var annotation = caller.getAnnotation(Prefix.class);
        if (annotation != null) {
            id = id.withPrefix(annotation.value() + "/");
        }

        var methods = builder.getClass().getDeclaredMethods();
        if (methods.length == 0) throw new IllegalArgumentException("Builder must be a functional interface");
        Method buildMethod = null;
        for (Method method : methods) {
            if (method.getName().equals("build") && method.getReturnType() == List.class) {
                buildMethod = method;
                break;
            }
        }

        if (buildMethod == null) throw new IllegalStateException("Builder must have a List<TopLevelMessageComponent> build(...) method");

        try {
            buildMethod.setAccessible(true);
            var methodHandle = LOOKUP.unreflect(buildMethod);
            BUILDERS.putIfAbsent(id, methodHandle.bindTo(builder));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return id;
    }

    /**
     * The return is for editing an interaction reply as it is expected that
     * it is an interaction reply and the reply was deferred
     *
     * @return the reply edit spec
     * @throws NullPointerException if there was no builder with the given ID
     */
    public static InteractionReplyEditSpec buildEditSpec(Identifier id, Object... args) {
        return InteractionReplyEditSpec.builder()
          .addAllComponents(buildComponents(id, args))
          .build()
          .withAllowedMentionsOrNull(AllowedMentions.suppressAll());
    }

    /**
     * The return is for editing an interaction reply as it is expected that
     * it is an interaction reply and the reply was deferred
     *
     * @return the reply edit spec
     * @throws NullPointerException if there was no builder with the given ID
     */
    @SuppressWarnings("unchecked")
    public static List<TopLevelMessageComponent> buildComponents(Identifier id, Object... args) {
        var messageBuilder = BUILDERS.get(id);
        if (messageBuilder == null) {
            throw new NullPointerException("Message builder %s does not exist".formatted(id));
        }

        try {
            return (List<TopLevelMessageComponent>) messageBuilder.invokeWithArguments(args);
        } catch (RuntimeException e) {
            throw e; // Rethrow, there is no need to catch them
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
