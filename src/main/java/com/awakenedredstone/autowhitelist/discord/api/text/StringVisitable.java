package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.util.Unit;

import java.util.Optional;

/**
 * An object that can supply strings to a visitor,
 * with or without a style context.
 */
public interface StringVisitable {
   /**
    * Convenience object indicating the termination of a string visit.
    */
   Optional<Unit> TERMINATE_VISIT = Optional.of(Unit.INSTANCE);
   /**
    * An empty visitable that does not call the visitors.
    */
   StringVisitable EMPTY = new StringVisitable() {
      public <T> Optional<T> visit(Visitor<T> visitor) {
         return Optional.empty();
      }
   };

   /**
    * Supplies this visitable's literal content to the visitor.
    *
    * @return {@code Optional.empty()} if the visit finished, or a terminating
    * result from the {@code visitor}
    *
    * @param visitor the visitor
    */
   <T> Optional<T> visit(Visitor<T> visitor);

   /**
    * Creates a visitable from a plain string.
    *
    * @param string the plain string
    */
   static StringVisitable plain(final String string) {
      return new StringVisitable() {
         public <T> Optional<T> visit(Visitor<T> visitor) {
            return visitor.accept(string);
         }
      };
   }

   default String getString() {
      StringBuilder stringBuilder = new StringBuilder();
      this.visit((string) -> {
         stringBuilder.append(string);
         return Optional.empty();
      });
      return stringBuilder.toString();
   }

   /**
    * A visitor for string content.
    */
   interface Visitor<T> {
      /**
       * Visits a literal string.
       *
       * <p>When a {@link Optional#isPresent() present optional} is returned,
       * the visit is terminated before visiting all text. Can return {@link
       * StringVisitable#TERMINATE_VISIT} for convenience.</p>
       *
       * @return {@code Optional.empty()} to continue, a non-empty result to terminate
       *
       * @param asString the literal string
       */
      Optional<T> accept(String asString);
   }

   /**
    * A visitor for string content and a contextual {@link Style}.
    */
   public interface StyledVisitor<T> {
      /**
       * Visits a string's content with a contextual style.
       *
       * <p>A contextual style is obtained by calling {@link Style#withParent(Style)}
       * on the current's text style, passing the previous contextual style or
       * the starting style if it is the beginning of a visit.
       *
       * <p>When a {@link Optional#isPresent() present optional} is returned,
       * the visit is terminated before visiting all text. Can return {@link
       * StringVisitable#TERMINATE_VISIT} for convenience.
       *
       * @return {@code Optional.empty()} to continue, a non-empty result to terminate
       *
       * @param style the current style
       * @param asString the literal string
       */
      Optional<T> accept(Style style, String asString);
   }
}
