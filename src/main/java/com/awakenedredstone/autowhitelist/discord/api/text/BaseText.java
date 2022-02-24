package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.util.Formatting;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public abstract class BaseText implements MutableText {
    protected final List<Text> siblings = Lists.newArrayList();
    private Style style;

    public BaseText() {
        this.style = Style.EMPTY;
    }

    public MutableText append(Text text) {
        this.siblings.add(text);
        return this;
    }

    public String asString() {
        return "";
    }

    public String markdownFormatted() {
        MutableText self = partialCopy();
        String text = self.getString();
        for (Formatting formatting : self.getStyle().formattings()) {
            switch (formatting) {
                case BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, CODE, CODE_BLOCK -> text = formatting.getMarkdown().concat(text).concat(formatting.getMarkdown());
            }
        }
        for (Text sibling : getSiblings()) {
            text = text.concat(sibling.markdownFormatted());
        }
        return text;
    }

    public final MutableText shallowCopy() {
        BaseText baseText = this.copy();
        baseText.siblings.addAll(this.siblings);
        baseText.setStyle(this.style);
        return baseText;
    }

    public final MutableText partialCopy() {
        BaseText baseText = this.copy();
        baseText.setStyle(this.style);
        return baseText;
    }

    public MutableText setStyle(Style style) {
        this.style = style;
        return this;
    }

    public Style getStyle() {
        return this.style;
    }

    public List<Text> getSiblings() {
        return this.siblings;
    }

    public abstract BaseText copy();

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BaseText baseText)) {
            return false;
        } else {
            return this.siblings.equals(baseText.siblings);
        }
    }

    public int hashCode() {
        return Objects.hash(this.siblings);
    }

    public String toString() {
        return "BaseComponent{siblings=" + this.siblings + '}';
    }
}
