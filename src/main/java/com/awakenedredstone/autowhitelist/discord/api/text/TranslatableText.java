package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.util.Util;
import com.awakenedredstone.autowhitelist.lang.JigsawLanguage;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslatableText extends BaseText implements ParsableText {
   private static final Object[] EMPTY_ARGUMENTS = new Object[0];
   private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
   private static final StringVisitable NULL_ARGUMENT = StringVisitable.plain("null");
   private final String key;
   private final Object[] args;
   @Nullable
   private Language languageCache;
   private final List<StringVisitable> translations = Lists.newArrayList();
   private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public TranslatableText(String key) {
      this.key = key;
      this.args = EMPTY_ARGUMENTS;
   }

   public TranslatableText(String key, Object... args) {
      this.key = key;
      this.args = args;
   }

   private void updateTranslations() {
      Language language = JigsawLanguage.getInstance();
      if (language != this.languageCache) {
         this.languageCache = language;
         this.translations.clear();
         String string = language.get(this.key);

         try {
            this.setTranslation(string);
         } catch (TranslationException var4) {
            this.translations.clear();
            this.translations.add(StringVisitable.plain(string));
         }

      }
   }

   private void setTranslation(String translation) {
      Matcher matcher = ARG_FORMAT.matcher(translation);

      try {
         int i = 0;

         int j;
         int l;
         for(j = 0; matcher.find(j); j = l) {
            int k = matcher.start();
            l = matcher.end();
            String string2;
            if (k > j) {
               string2 = translation.substring(j, k);
               if (string2.indexOf(37) != -1) {
                  throw new IllegalArgumentException();
               }

               this.translations.add(StringVisitable.plain(string2));
            }

            string2 = matcher.group(2);
            String string3 = translation.substring(k, l);
            if ("%".equals(string2) && "%%".equals(string3)) {
               this.translations.add(LITERAL_PERCENT_SIGN);
            } else {
               if (!"s".equals(string2)) {
                  throw new TranslationException(this, "Unsupported format: '" + string3 + "'");
               }

               String string4 = matcher.group(1);
               int m = string4 != null ? Integer.parseInt(string4) - 1 : i++;
               if (m < this.args.length) {
                  this.translations.add(this.getArg(m));
               }
            }
         }

         if (j < translation.length()) {
            String string5 = translation.substring(j);
            if (string5.indexOf(37) != -1) {
               throw new IllegalArgumentException();
            }

            this.translations.add(StringVisitable.plain(string5));
         }

      } catch (IllegalArgumentException var11) {
         throw new TranslationException(this, var11);
      }
   }

   public MutableText parse(@Nullable DiscordCommandSource source, @Nullable User sender, int depth) throws CommandSyntaxException {
      Object[] objects = new Object[this.args.length];

      for(int i = 0; i < objects.length; ++i) {
         Object object = this.args[i];
         if (object instanceof Text) {
            objects[i] = Util.parse(source, (Text)object, sender, depth);
         } else {
            objects[i] = object;
         }
      }

      return new TranslatableText(this.key, objects);
   }

   private StringVisitable getArg(int index) {
      if (index >= this.args.length) {
         throw new TranslationException(this, index);
      } else {
         Object object = this.args[index];
         if (object instanceof Text) {
            return (Text)object;
         } else {
            return object == null ? NULL_ARGUMENT : StringVisitable.plain(object.toString());
         }
      }
   }

   public TranslatableText copy() {
      return new TranslatableText(this.key, this.args);
   }

   public <T> Optional<T> visitSelf(StringVisitable.Visitor<T> visitor) {
      this.updateTranslations();
      Iterator<StringVisitable> var2 = this.translations.iterator();

      Optional<T> optional;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         StringVisitable stringVisitable = var2.next();
         optional = stringVisitable.visit(visitor);
      } while(optional.isEmpty());

      return optional;
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (!(object instanceof TranslatableText translatableText)) {
         return false;
      } else {
         return Arrays.equals(this.args, translatableText.args) && this.key.equals(translatableText.key) && super.equals(object);
      }
   }

   public int hashCode() {
      int i = super.hashCode();
      i = 31 * i + this.key.hashCode();
      i = 31 * i + Arrays.hashCode(this.args);
      return i;
   }

   public String toString() {
      return "TranslatableComponent{key='" + this.key + "', args=" + Arrays.toString(this.args) + ", siblings=" + this.siblings + "}";
   }
}
