package com.awakenedredstone.autowhitelist.discord.command.admin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserInfoCommand extends SlashCommand {
    public UserInfoCommand() {
        this.name = "userinfo";
        this.help = Text.translatable("discord.command.description.userinfo").getString();

        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};

        this.options.add(new OptionData(OptionType.USER, "user", Text.translatable("discord.command.description.userinfo.argument").getString()).setRequired(false));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        ReplyCallback.InteractionReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
            }
        };

        replyCallback.sendMessage(null);

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
        Member member = event.getOption("user", event.getMember(), OptionMapping::getAsMember);

        if (member == null) {
            AutoWhitelist.LOGGER.error("Member is null", new IllegalStateException());
            MessageEmbed embed = DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.fatal.title"),
              Text.translatable("discord.command.fatal.generic", "Member is null"),
              DiscordBotHelper.MessageType.FATAL
            );

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed))
            );
            return;
        }

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(member.getId(), whitelist);

        List<Role> roles = DiscordBotHelper.getRolesForMember(member);
        boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));

        EmbedBuilder embed = DiscordBotHelper.Feedback.defaultEmbed(
          Text.translatable("discord.command.userinfo.title"),
          Text.translatable("discord.command.userinfo.description", member.getAsMention())
        );

        embed.addField(Text.translatable("discord.command.userinfo.roles").getString(), String.join(" ", roles.stream().map(IMentionable::getAsMention).toList()), false);
        embed.addField(Text.translatable("discord.command.userinfo.whitelisted").getString(), String.valueOf(whitelistedAccount.isPresent()), true);
        embed.addField(Text.translatable("discord.command.userinfo.qualifies").getString(), String.valueOf(accepted), true);

        if (whitelistedAccount.isPresent()) {
            ExtendedWhitelistEntry entry = whitelistedAccount.get();
            ExtendedGameProfile profile = entry.getProfile();
            String[] fields = new String[]{"username", "role", "lock"};
            for (String field : fields) {
                Text title = Text.translatable("discord.command.info.field.%s.title".formatted(field));
                if (title.getString().isEmpty()) continue;

                String descriptionKey = "discord.command.info.field.%s.description".formatted(field);
                Text description = switch (field) {
                    case "username" -> Text.translatable(descriptionKey, profile.getName());
                    case "role" -> Text.translatable(descriptionKey, "<@&" + profile.getRole() + ">");
                    case "lock" -> {
                        String time = "future";
                        if (profile.getLockedUntil() == -1) {
                            time = "permanent";
                        } else if (profile.getLockedUntil() <= System.currentTimeMillis()) {
                            time = "past";
                        }
                        String timeKey = "." + time;
                        yield Text.translatable(descriptionKey + timeKey, DiscordBotHelper.formatDiscordTimestamp(profile.getLockedUntil()));
                    }
                    default -> Text.of("");
                };
                if (description.getString().isEmpty()) continue;

                embed.addField(title.getString(), description.getString(), true);
            }
        }

        replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
          .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed.build()))
        );
    }
}
