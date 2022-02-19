package com.awakenedredstone.autowhitelist.discord.api.text;

public class LiteralText extends BaseText {
   public static final Text EMPTY = new LiteralText("");
   private final String string;

   public LiteralText(String string) {
      this.string = string;
   }

   public String getRawString() {
      return this.string;
   }

   public String asString() {
      return this.string;
   }

   public LiteralText copy() {
      return new LiteralText(this.string);
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (!(object instanceof LiteralText literalText)) {
         return false;
      } else {
         return this.string.equals(literalText.getRawString()) && super.equals(object);
      }
   }

   public String toString() {
      return "TextComponent{text='" + this.string + "', siblings=" + this.siblings + "}";
   }
}
