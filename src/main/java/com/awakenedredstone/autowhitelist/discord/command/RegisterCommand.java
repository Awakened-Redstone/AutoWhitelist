package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.mixin.UserCacheAccessor;
import com.awakenedredstone.autowhitelist.util.Validation;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
/*? if <1.20.2 {*//*import com.mojang.authlib.Agent;*//*?}*/
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class RegisterCommand extends SlashCommand {
    public RegisterCommand() {
        this.name = "register";
        this.help = Text.translatable("discord.command.description.register").getString();
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};

        this.options.add(new OptionData(OptionType.STRING, "username", Text.translatable("command.description.register.argument").getString()).setRequired(true));
    }

    public static void execute(@NotNull Member member, @NotNull String username, @NotNull ReplyCallback.InteractionReplyCallback replyCallback) {
        MessageCreateData initialReply = DiscordBotHelper.buildEmbedMessage(
          false,
          DiscordBotHelper.Feedback.buildEmbed(
            Text.translatable("discord.command.feedback.received.title"),
            Text.translatable("discord.command.feedback.received.message"),
            DiscordBotHelper.MessageType.NORMAL
          )
        );

        replyCallback.sendMessage(initialReply);

        String id = member.getId();

        Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(member);
        if (highestRole.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.fail.not_allowed.title"),
                  Text.translatable("discord.command.register.fail.not_allowed.message"),
                  DiscordBotHelper.MessageType.NORMAL
                )
              )
            );

            return;
        }

        MinecraftServer server = AutoWhitelist.getServer();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

        Optional<ExtendedWhitelistEntry> whitelistedAccount = getWhitelistedAccount(id, whitelist);
        if (whitelistedAccount.isPresent()) {
            if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(whitelistedAccount.get().getProfile())) {
                replyCallback.editMessage(
                  DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.register.player_banned.title"),
                      Text.translatable("discord.command.register.player_banned.message"),
                      DiscordBotHelper.MessageType.INFO
                    )
                  )
                );
                return;
            }

            if (whitelistedAccount.get().getProfile().getName().equalsIgnoreCase(username)) {
                replyCallback.editMessage(
                  DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.register.same_username.title"),
                      Text.translatable("discord.command.register.same_username.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }

            if (whitelistedAccount.get().getProfile().isLocked()) {
                if (AutoWhitelist.CONFIG.lockTime() == -1) {
                    replyCallback.editMessage(
                      DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("command.register.already_registered.title"),
                          Text.translatable("command.register.already_registered.message"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                } else {
                    replyCallback.editMessage(
                      DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("command.register.locked.title"),
                          Text.translatable("command.register.locked.message", DiscordBotHelper.formatDiscordTimestamp(whitelistedAccount.get().getProfile().getLockedUntil())),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                }
                return;
            }
        }

        BaseEntryAction entry = RoleActionMap.get(highestRole.get());
        if (!entry.isValid()) {
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
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

        if (username.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.few_args.title"),
                  Text.translatable("discord.command.few_args.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        if (!Validation.isValidMinecraftUsername(username)) {
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.invalid_username.title"),
                  Text.translatable("discord.command.register.invalid_username.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        if (server.getUserCache() == null) {
            AutoWhitelist.LOGGER.error("Failed to whitelist user {}, server user cache is null", username);
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
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
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.register.fail.account_data", username),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }
        ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole.get().getId(), id, AutoWhitelist.CONFIG.lockTime());
        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
            replyCallback.editMessage(
              DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.player_banned.title"),
                  Text.translatable("discord.command.register.player_banned.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }
        replyCallback.editMessage(
          DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.last_steps.title"),
              Text.translatable("discord.command.register.last_steps.message"),
              DiscordBotHelper.MessageType.INFO
            )
          )
        );

        whitelistPlayer(whitelistedAccount.orElse(null), whitelist, extendedProfile, entry);

        replyCallback.editMessage(
          DiscordBotHelper.<MessageEditData>buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.success.title"),
              Text.translatable("discord.command.register.success.message"),
              DiscordBotHelper.MessageType.SUCCESS
            )
          )
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        ReplyCallback.InteractionReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
            }
        };

        @NotNull Member member = Objects.requireNonNull(event.getMember());
        @NotNull String username = Objects.requireNonNull(event.getOption("username", OptionMapping::getAsString));

        execute(member, username, replyCallback);
    }

    public static void whitelistPlayer(ExtendedWhitelistEntry whitelistedAccount, ExtendedWhitelist whitelist, ExtendedGameProfile extendedProfile, BaseEntryAction entry) {
        if (whitelistedAccount != null) {
            whitelist.remove(whitelistedAccount.getProfile());
            if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                AutoWhitelist.WHITELIST_CACHE.remove(new WhitelistCacheEntry(whitelistedAccount.getProfile()));
            }
        }

        whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
        if (AutoWhitelist.CONFIG.enableWhitelistCache) {
            WhitelistCacheEntry cacheEntry;
            if ((cacheEntry = AutoWhitelist.WHITELIST_CACHE.getFromId(extendedProfile.getDiscordId())) != null) {
                AutoWhitelist.WHITELIST_CACHE.remove(cacheEntry);
            }
            AutoWhitelist.WHITELIST_CACHE.add(new WhitelistCacheEntry(extendedProfile));
        }
        AutoWhitelist.LOGGER.debug("Whitelisting {} for entry of type {} (Role: {})", extendedProfile.getName(), entry.getType(), extendedProfile.getRole());
        entry.registerUser(extendedProfile);
    }

    @NotNull
    public static Optional<ExtendedWhitelistEntry> getWhitelistedAccount(String id, @NotNull ExtendedWhitelist whitelist) {
        return whitelist.getEntries().stream().filter(entry -> {
            try {
                ExtendedWhitelistEntry whitelistEntry = (ExtendedWhitelistEntry) entry;
                return whitelistEntry.getProfile().getDiscordId().equals(id);
            } catch (Throwable e) {
                return false;
            }
        }).map(e -> (ExtendedWhitelistEntry) e).findFirst();
    }
}
