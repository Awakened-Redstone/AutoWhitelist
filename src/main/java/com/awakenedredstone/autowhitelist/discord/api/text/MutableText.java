package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.util.Formatting;

/**
 * A text with mutation operations.
 */
public interface MutableText extends Text {
   /**
    * Sets the style of this text.
    */
   MutableText setStyle(Style style);

   Style getStyle();

   default MutableText fillStyle(Style styleOverride) {
      this.setStyle(styleOverride.withParent(this.getStyle()));
      return this;
   }

   /**
    * Appends a literal text with content {@code text} to this text's siblings.
    * 
    * @param text the literal text content
    */
   default MutableText append(String text) {
      return this.append(new LiteralText(text));
   }

   /**
    * Appends a text to this text's siblings.
    * 
    * @param text the sibling
    */
   MutableText append(Text text);

   /**
    * Adds some formattings to this text's style.
    *
    * @param formattings an array of formattings
    */
   default MutableText formatted(Formatting... formattings) {
      this.setStyle(this.getStyle().withFormatting(formattings));
      return this;
   }

   /**
    * Add a formatting to this text's style.
    *
    * @param formatting a formatting
    */
   default MutableText formatted(Formatting formatting) {
      this.setStyle(this.getStyle().withFormatting(formatting));
      return this;
   }
}
