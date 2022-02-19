package com.awakenedredstone.autowhitelist.discord.api.text;

import com.mojang.brigadier.Message;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A text. Can be converted to and from JSON format.
 * 
 * <p>Each text has a tree structure, embodying all its {@link
 * #getSiblings() siblings}. To iterate contents in the text and all
 * its siblings, call {@code visit} methods.</p>
 * 
 * <p>This interface does not expose mutation operations. For mutation,
 * refer to {@link MutableText}.</p>
 * 
 * @see MutableText
 */
public interface Text extends Message, StringVisitable {
   /**
    * Returns the style of this text.
    */
   Style getStyle();

   /**
    * Returns the string representation of this text itself, excluding siblings.
    */
   String asString();

   default String getString() {
      return StringVisitable.super.getString();
   }

   /**
    * Returns the string representation of this text itself, transforming its {@link Style} to markdown format.
    */
   String markdownFormatted();

   /**
    * Returns the siblings of this text.
    */
   List<Text> getSiblings();

   MutableText shallowCopy();

   MutableText partialCopy();

   /**
    * Copies the text itself, excluding the styles or siblings.
    */
   MutableText copy();

   default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
      Style style2 = this.getStyle().withParent(style);
      Optional<T> optional = this.visitSelf(styledVisitor, style2);
      if (optional.isPresent()) {
         return optional;
      } else {
         Iterator<Text> var5 = this.getSiblings().iterator();

         Optional<T> optional2;
         do {
            if (!var5.hasNext()) {
               return Optional.empty();
            }

            Text text = var5.next();
            optional2 = text.visit(styledVisitor, style2);
         } while(optional2.isEmpty());

         return optional2;
      }
   }

   default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
      Optional<T> optional = this.visitSelf(visitor);
      if (optional.isPresent()) {
         return optional;
      } else {
         Iterator<Text> var3 = this.getSiblings().iterator();

         Optional<T> optional2;
         do {
            if (!var3.hasNext()) {
               return Optional.empty();
            }

            Text text = var3.next();
            optional2 = text.visit(visitor);
         } while(optional2.isEmpty());

         return optional2;
      }
   }

   /**
    * Visits the text itself.
    *
    * @see #visit(StyledVisitor, Style)
    * @return the visitor's return value
    *
    * @param visitor the visitor
    * @param style the current style
    */
   default <T> Optional<T> visitSelf(StringVisitable.StyledVisitor<T> visitor, Style style) {
      return visitor.accept(style, this.asString());
   }

   /**
    * Visits the text itself.
    * 
    * @see #visit(Visitor)
    * @return the visitor's return value
    * 
    * @param visitor the visitor
    */
   default <T> Optional<T> visitSelf(StringVisitable.Visitor<T> visitor) {
      return visitor.accept(this.asString());
   }

}
