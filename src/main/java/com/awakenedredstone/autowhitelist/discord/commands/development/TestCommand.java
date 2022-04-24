package com.awakenedredstone.autowhitelist.discord.commands.development;

import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.text.LiteralText;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.awakenedredstone.autowhitelist.discord.api.util.Formatting;
import com.mojang.brigadier.CommandDispatcher;
import net.dv8tion.jda.api.EmbedBuilder;

public class TestCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("test").requires((source) -> {
            return source.isFromGuild() && source.getMember().getRoles().size() == 2;
        }).executes((source) -> {
            execute(source.getSource());
            return 0;
        }));

//        CommandData command = new CommandDataImpl("test", new TranslatableText("command.description.test").markdownFormatted());
//        jda.upsertCommand(command).queue();
    }

    public static void execute(DiscordCommandSource source) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String description = "";
        description +=        new LiteralText("Bold").formatted(Formatting.BOLD).markdownFormatted();
        description += "\n" + new LiteralText("Italic").formatted(Formatting.ITALIC).markdownFormatted();
        description += "\n" + new LiteralText("Strikethrough").formatted(Formatting.STRIKETHROUGH).markdownFormatted();
        description += "\n" + new LiteralText("Underline").formatted(Formatting.UNDERLINE).markdownFormatted();
        description += "\n" + new LiteralText("Code").formatted(Formatting.CODE).markdownFormatted();
        description += "\n" + new LiteralText("Code block").formatted(Formatting.CODE_BLOCK).markdownFormatted();
        description += "\n" + new LiteralText("Italic Bold").formatted(Formatting.ITALIC, Formatting.BOLD).markdownFormatted();
        description += "\n" + new LiteralText("Almost all").formatted(Formatting.CODE, Formatting.BOLD, Formatting.ITALIC, Formatting.STRIKETHROUGH, Formatting.UNDERLINE).markdownFormatted();
        description += "\n" + new LiteralText("All").formatted(Formatting.CODE, Formatting.BOLD, Formatting.ITALIC, Formatting.STRIKETHROUGH, Formatting.UNDERLINE, Formatting.CODE_BLOCK).markdownFormatted();
        description += "\n" + new TranslatableText("test.badTranslation").markdownFormatted();
        description += "\n" + new TranslatableText("test.translation.arg").markdownFormatted();
        description += "\n" + new TranslatableText("test.translation.args").markdownFormatted();
        description += "\n" + new TranslatableText("test.translation").markdownFormatted();
        description += "\n" + new TranslatableText("test.translation.arg", "\"this is an arg\"").markdownFormatted();
        description += "\n" + new TranslatableText("test.translation.args", "\"this is an arg\"", "\"and this is another\"").markdownFormatted();
        description += "\n" + new LiteralText("Test complete, I think.").markdownFormatted();
        embedBuilder.setDescription(description);
        if (source.getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
//            ((SlashCommandInteractionEvent)source.getEvent()).replyEmbeds(embedBuilder.build()).queue();
        } else {
            source.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }
}
