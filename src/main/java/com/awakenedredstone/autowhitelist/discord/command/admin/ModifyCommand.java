package com.awakenedredstone.autowhitelist.discord.command.admin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.mixin.UserCacheAccessor;
import com.awakenedredstone.autowhitelist.util.Validation;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
/*? if <1.20.2 {*//*import com.mojang.authlib.Agent;*//*?}*/
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class ModifyCommand extends SlashCommand {
    public ModifyCommand() {
        this.name = "modify";
        this.help = Text.translatable("discord.command.description.admin/modify").getString();

        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};

        this.options.add(new OptionData(OptionType.USER, "user", Text.translatable("discord.command.description.admin/modify.argument/user").getString()).setRequired(true));
        this.options.add(new OptionData(OptionType.STRING, "username", Text.translatable("discord.command.description.admin/modify.argument/username").getString()).setRequired(true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        ReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
            }
        };

        MessageCreateData initialReply = DiscordBotHelper.buildEmbedMessage(
          false,
          DiscordBotHelper.Feedback.buildEmbed(
            Text.translatable("discord.command.feedback.received.title"),
            Text.translatable("discord.command.feedback.received.message"),
            DiscordBotHelper.MessageType.NORMAL
          )
        );

        replyCallback.sendMessage(initialReply);

        @NotNull Member member = Objects.requireNonNull(event.getOption("user", OptionMapping::getAsMember));
        @NotNull String username = Objects.requireNonNull(event.getOption("username", OptionMapping::getAsString));

        String id = member.getId();

        Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(member);

        if (highestRole.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.fail.not_allowed.title"),
                  Text.translatable("discord.command.modify.fail.not_allowed.message"),
                  DiscordBotHelper.MessageType.NORMAL
                )
              )
            );

            return;
        }

        MinecraftServer server = AutoWhitelist.getServer();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(id, whitelist);
        if (whitelistedAccount.isPresent()) {
            if (whitelistedAccount.get().getProfile().getName().equalsIgnoreCase(username)) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.modify.same_username.title"),
                      Text.translatable("discord.command.modify.same_username.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
        }

        BaseEntryAction entry = RoleActionMap.get(highestRole.get());
        if (!entry.isValid()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.fatal.generic", "Failed to validate entry action"),
                  DiscordBotHelper.MessageType.FATAL
                )
              )
            );
            AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to validate entry {} but failed", entry);
            return;
        }

        if (!Validation.isValidMinecraftUsername(username)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.invalid_username.title"),
                  Text.translatable("discord.command.modify.invalid_username.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        if (server.getUserCache() == null) {
            AutoWhitelist.LOGGER.error("Failed to whitelist user {}, server user cache is null", username);
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.register.fatal", "Failed to whitelist user %s, server user cache is null".formatted(username)),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        //noinspection DuplicatedCode
        if (DebugFlags.trackEntryError) {
            ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback(){

                @Override
                public void onProfileLookupSucceeded(GameProfile profile) {
                    AutoWhitelist.LOGGER.info("Successfully got user profile {}" ,profile);
                }

                @Override
                public void onProfileLookupFailed(/*? if >=1.20.2 {*/String/*?} else {*//*GameProfile*//*?}*/ name, Exception exception) {
                    AutoWhitelist.LOGGER.error("Failed to get user profile for {}", name, exception);
                }
            };

            ((UserCacheAccessor) server.getUserCache()).getProfileRepository().findProfilesByNames(new String[]{username}/*? if <1.20.2 {*//*, Agent.MINECRAFT*//*?}*/, profileLookupCallback);
        }

        GameProfile profile = server.getUserCache().findByName(username).orElse(null);
        if (profile == null) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.modify.fail.account_data", username),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }
        ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole.get().getId(), id, AutoWhitelist.CONFIG.lockTime());
        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.player_banned.title"),
                  Text.translatable("discord.command.modify.player_banned.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }
        boolean whitelisted = whitelist.isAllowed(extendedProfile);
        if (whitelisted) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.username_already_registered.title"),
                  Text.translatable("discord.command.modify.username_already_registered.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
        } else {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.last_steps.title"),
                  Text.translatable("discord.command.modify.last_steps.message"),
                  DiscordBotHelper.MessageType.INFO
                )
              )
            );

            RegisterCommand.whitelistPlayer(whitelistedAccount.orElse(null), whitelist, extendedProfile, entry);

            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.success.title", member.getEffectiveName()),
                  Text.translatable("discord.command.modify.success.message", member.getAsMention(), username),
                  DiscordBotHelper.MessageType.SUCCESS
                )
              )
            );
        }
    }
}
