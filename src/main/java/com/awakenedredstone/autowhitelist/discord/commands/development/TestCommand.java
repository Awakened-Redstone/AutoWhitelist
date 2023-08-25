package com.awakenedredstone.autowhitelist.discord.commands.development;

import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.util.Markdown;
import com.awakenedredstone.autowhitelist.util.TextUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TestCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("test").requires((source) -> {
            return source.isFromGuild() && source.getMember().getRoles().size() == 2;
        }).executes((source) -> {
            execute(source.getSource());
            return 0;
        }));

//        CommandData command = new CommandDataImpl("test", Text.translatable("command.description.test"));
//        jda.upsertCommand(command).queue();
    }

    public static void execute(DiscordCommandSource source) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String description = "";
        description += Markdown.formatText(TextUtil.placeholder(Text.literal("Bold").formatted(Formatting.BOLD)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("Italic").formatted(Formatting.ITALIC)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("Strikethrough").formatted(Formatting.STRIKETHROUGH)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("Underline").formatted(Formatting.UNDERLINE)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("Italic Bold").formatted(Formatting.ITALIC, Formatting.BOLD)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("All").formatted(Formatting.BOLD, Formatting.ITALIC, Formatting.STRIKETHROUGH, Formatting.UNDERLINE)));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.badTranslation")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.translation.arg")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.translation.args")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.translation")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.translation.arg", "\"this is an arg\"")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.translatable("test.translation.args", "\"this is an arg\"", "\"and this is another\"")));
        description += "\n" + Markdown.formatText(TextUtil.placeholder(Text.literal("Test complete!")));
        embedBuilder.setDescription(description);

        source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
