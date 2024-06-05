package com.awakenedredstone.autowhitelist.mixin.compat;

import blue.endless.jankson.annotation.SerializedName;
import blue.endless.jankson.impl.POJODeserializer;
import com.awakenedredstone.autowhitelist.config.source.annotation.NameFormat;
import com.awakenedredstone.autowhitelist.config.source.annotation.SkipNameFormat;
import com.google.common.base.CaseFormat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Field;

@Mixin(value = POJODeserializer.class, remap = false)
public class POJODeserializerMixin {
    @ModifyExpressionValue(method = "unpackField", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Field;getName()Ljava/lang/String;"))
    private static String parseName(String original, @Local(argsOnly = true) Object parent, @Local(argsOnly = true) Field field) {
        NameFormat defaultNameFormat = parent.getClass().getAnnotation(NameFormat.class);

        NameFormat nameFormat = field.getAnnotation(NameFormat.class);
        SkipNameFormat skipNameFormat = field.getAnnotation(SkipNameFormat.class);
        SerializedName nameAnnotation = field.getAnnotation(SerializedName.class);
        if (skipNameFormat == null && (nameFormat != null || defaultNameFormat != null) && nameAnnotation == null) {
            NameFormat formatter = nameFormat != null ? nameFormat : defaultNameFormat;
            return CaseFormat.LOWER_CAMEL.to(formatter.value().getCaseFormat(), original);
        }

        return original;
    }
}
