package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandBase;
import com.awakenedredstone.autowhitelist.mixin.UserCacheAccessor;
import com.awakenedredstone.autowhitelist.util.Validation;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
/*? if <1.20.2 {*//*import com.mojang.authlib.Agent;*//*?}*/
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static net.dv8tion.jda.api.Permission.MESSAGE_ATTACH_FILES;
import static net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS;
import static net.dv8tion.jda.api.Permission.MESSAGE_HISTORY;
import static net.dv8tion.jda.api.Permission.MESSAGE_SEND;
import static net.dv8tion.jda.api.Permission.VIEW_CHANNEL;

public class RegisterCommand extends CommandBase {
    public static final RegisterCommand INSTANCE = new RegisterCommand();

    public RegisterCommand() {
        this.name = "register";
        this.description = Text.translatable("discord.command.description.register").getString();
    }

    public static void execute(Member member, String username, ReplyCallback replyCallback) {
        if (member == null) return;

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
        List<Role> roles = DiscordBotHelper.getRolesForMember(member);

        boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));
        if (!accepted) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
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
                  DiscordBotHelper.buildEmbedMessage(true,
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
                  DiscordBotHelper.buildEmbedMessage(true,
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
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("command.register.already_registered.title"),
                          Text.translatable("command.register.already_registered.message"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                } else {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
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

        Optional<String> highestRoleOptional = DiscordDataProcessor.getTopRole(roles);
        if (highestRoleOptional.isEmpty()) {
            AutoWhitelist.LOGGER.error("Impossible case, the user {} has no valid role, but it passed as qualified. Please report this bug.", id, new IllegalStateException());
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.fatal.title"),
                  Text.translatable("discord.command.register.fatal", "User does not have a valid role, yet it passed as qualified. Please report this bug."),
                  DiscordBotHelper.MessageType.FATAL
                )
              )
            );
            return;
        }

        String highestRole = highestRoleOptional.get();
        BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(highestRole);
        try {
            entry.assertValid();
        } catch (Throwable e) {
            replyCallback.editMessage(
                DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.fail.title"),
                      Text.translatable("discord.command.fatal.exception", e.getMessage()),
                    DiscordBotHelper.MessageType.FATAL
                    )
                )
            );
            AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to assert entry but got an exception", e);
            return;
        }

        if (username.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
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
              DiscordBotHelper.buildEmbedMessage(true,
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
                  Text.translatable("discord.command.register.fail.account_data", username),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }
        ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole, id, AutoWhitelist.CONFIG.lockTime());
        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
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
          DiscordBotHelper.buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.last_steps.title"),
              Text.translatable("discord.command.register.last_steps.message"),
              DiscordBotHelper.MessageType.INFO
            )
          )
        );

        whitelistPlayer(whitelistedAccount.orElse(null), whitelist, extendedProfile, entry);

        replyCallback.editMessage(
          DiscordBotHelper.buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.success.title"),
              Text.translatable("discord.command.register.success.message"),
              DiscordBotHelper.MessageType.SUCCESS
            )
          )
        );
    }

    public static void whitelistPlayer(ExtendedWhitelistEntry whitelistedAccount, ExtendedWhitelist whitelist, ExtendedGameProfile extendedProfile, BaseEntry entry) {
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

    @Deprecated(since = "1.0.0-beta.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "1.1.0")
    public class TextCommand extends Command {
        public TextCommand() {
            this.name = RegisterCommand.this.name;
            this.help = RegisterCommand.this.description;
            this.category = new Category("Server integration");
            this.botPermissions = new Permission[]{MESSAGE_ATTACH_FILES, MESSAGE_HISTORY, MESSAGE_EMBED_LINKS, VIEW_CHANNEL, MESSAGE_SEND};
            this.arguments = "<minecraft username>";
            this.guildOnly = true;
        }

        @Override
        protected void execute(CommandEvent event) {
            MessageCreateData deprecationWarning = DiscordBotHelper.buildEmbedMessage(false,
              DiscordBotHelper.Feedback.buildEmbed(
                Text.literal("Text commands are deprecated!"),
                Text.literal("Please move into using the slash commands as text commands are deprecated and will be removed on version 1.1.0"),
                DiscordBotHelper.MessageType.ERROR
              )
            );

            event.getMessage().reply(deprecationWarning).mentionRepliedUser(true).submit();

            RegisterCommand.execute(event.getMember(), event.getArgs(), new ReplyCallback() {
                private final Queue<MessageEditData> queue = new LinkedList<>();
                private boolean processing = false;
                private Message message;

                @Override
                public void sendMessage(MessageCreateData message) {
                    queueMessage(event.getMessage().reply(message).mentionRepliedUser(false));
                }

                @Override
                public void editMessage(MessageEditData message) {
                    if (!this.processing) {
                        queueMessage(this.message.editMessage(message));
                    } else {
                        queue.add(message);
                    }
                }

                private void queueMessage(MessageCreateAction message) {
                    this.processing = true;
                    message.queue(message1 -> {
                        this.message = message1;
                        this.processing = false;
                        if (!queue.isEmpty()) {
                            queueMessage(message1.editMessage(queue.poll()));
                        }
                    });
                }

                private void queueMessage(MessageEditAction message) {
                    this.processing = true;
                    message.queue(message1 -> {
                        this.message = message1;
                        this.processing = false;
                        if (!queue.isEmpty()) {
                            queueMessage(message1.editMessage(queue.poll()));
                        }
                    });
                }
            });
        }
    }

    public class SlashCommand extends com.jagrosh.jdautilities.command.SlashCommand {
        public SlashCommand() {
            this.name = RegisterCommand.this.name;
            this.help = RegisterCommand.this.description;
            this.guildOnly = true;

            this.options.add(new OptionData(OptionType.STRING, "username", Text.translatable("command.description.register.argument").getString()).setRequired(true));
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            //noinspection DataFlowIssue
            RegisterCommand.execute(event.getMember(), event.getOption("username").getAsString(), new ReplyCallback.InteractionReplyCallback() {

                @Override
                public void acknowledge() {
                    lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
                }
            });
        }
    }
}
