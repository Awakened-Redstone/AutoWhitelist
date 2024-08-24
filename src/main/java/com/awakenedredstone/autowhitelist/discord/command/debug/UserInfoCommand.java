package com.awakenedredstone.autowhitelist.discord.command.debug;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
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
        this.name = "debug-userinfo";
        this.help = Stonecutter.translatableText("command.description.debug/userinfo").getString();

        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};

        this.options.add(new OptionData(OptionType.USER, "user", Stonecutter.translatedText("command.description.debug/userinfo.argument")).setRequired(false));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        boolean ephemeral = AutoWhitelist.CONFIG.ephemeralReplies;
        ReplyCallback.InteractionReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(ephemeral).submit();
            }
        };

        replyCallback.sendMessage(null);

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
        Member member;
        OptionMapping user = event.getOption("user");
        if (user != null) {
            member = user.getAsMember();
        } else {
            member = event.getMember();
        }

        if (member == null) {
            EmbedBuilder embed = DiscordBotHelper.Feedback.defaultEmbed(
              Stonecutter.translatableText("internal_error.title"),
              Stonecutter.translatableText("internal_error.description")
            );

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed.build()))
            );
            return;
        }

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(member.getId(), whitelist);

        List<Role> roles = DiscordBotHelper.getRolesForMember(member);
        boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));

        EmbedBuilder embed = DiscordBotHelper.Feedback.defaultEmbed(
          Stonecutter.translatableText("command.debug/userinfo.title"),
          Stonecutter.translatableText("command.debug/userinfo.description", member.getAsMention())
        );

        embed.addField(Stonecutter.translatedText("command.debug/userinfo.roles"), String.join(" ", roles.stream().map(IMentionable::getAsMention).toList()), false);
        embed.addField(Stonecutter.translatedText("command.debug/userinfo.whitelisted"), String.valueOf(whitelistedAccount.isPresent()), true);
        embed.addField(Stonecutter.translatedText("command.debug/userinfo.qualifies"), String.valueOf(accepted), true);

        if (whitelistedAccount.isPresent()) {
            ExtendedWhitelistEntry entry = whitelistedAccount.get();
            ExtendedGameProfile profile = entry.getProfile();
            String[] fields = new String[]{"username", "role", "lock"};
            for (String field : fields) {
                Text title = Stonecutter.translatableText("command.info.field.%s.title".formatted(field));
                if (title.getString().isEmpty()) continue;

                String descriptionKey = "command.info.field.%s.description".formatted(field);
                Text description = switch (field) {
                    case "username" -> Stonecutter.translatableText(descriptionKey, profile.getName());
                    case "role" -> Stonecutter.translatableText(descriptionKey, "<@&" + profile.getRole() + ">");
                    case "lock" -> {
                        String time = "future";
                        if (profile.getLockedUntil() == -1) {
                            time = "permanent";
                        } else if (profile.getLockedUntil() <= System.currentTimeMillis()) {
                            time = "past";
                        }
                        String timeKey = "." + time;
                        yield Stonecutter.translatableText(descriptionKey + timeKey, DiscordBotHelper.formatDiscordTimestamp(profile.getLockedUntil()));
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
