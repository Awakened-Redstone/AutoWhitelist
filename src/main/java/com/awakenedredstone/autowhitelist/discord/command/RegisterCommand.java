package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandBase;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.mojang.authlib.GameProfile;
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
/*? if >=1.19 {*//*
import net.minecraft.text.Text;
*//*?} else {*/
import net.minecraft.text.TranslatableText;
/*?} */
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

import static net.dv8tion.jda.api.Permission.*;

public class RegisterCommand extends CommandBase {
    public static final RegisterCommand INSTANCE = new RegisterCommand();

    public RegisterCommand() {
        this.name = "register";
        this.description = /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.description.register").getString();
    }

    public static void execute(Member member, String username, ReplyCallback replyCallback) {
        if (member == null) return;

        MessageCreateData initialReply = BotHelper.buildEmbedMessage(
          false,
            BotHelper.Feedback.buildEmbed(
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.received.title"),
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.received.message"),
              BotHelper.MessageType.NORMAL
            )
        );

        replyCallback.sendMessage(initialReply);

        String id = member.getId();
        List<Role> roles = BotHelper.getRolesForMember(member);

        boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));
        if (accepted) {
            MinecraftServer server = AutoWhitelist.getServer();
            ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

            Optional<ExtendedWhitelistEntry> whitelistedAccount = whitelist.getEntries().stream().filter(entry -> {
                try {
                    ExtendedWhitelistEntry whitelistEntry = (ExtendedWhitelistEntry) entry;
                    return whitelistEntry.getProfile().getDiscordId().equals(id);
                } catch (Throwable e) {
                    return false;
                }
            }).map(e -> (ExtendedWhitelistEntry) e).findFirst();
            if (whitelistedAccount.isPresent()) {
                if (whitelistedAccount.get().getProfile().getName().equalsIgnoreCase(username)) {
                    replyCallback.editMessage(
                      BotHelper.buildEmbedMessage(true,
                        BotHelper.Feedback.buildEmbed(
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.same_username.title"),
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.same_username.message"),
                          BotHelper.MessageType.WARNING
                        )
                      )
                    );
                    return;
                }

                if (whitelistedAccount.get().getProfile().isLocked()) {
                    if (AutoWhitelist.CONFIG.lockTime() == -1) {
                        replyCallback.editMessage(
                          BotHelper.buildEmbedMessage(true,
                            BotHelper.Feedback.buildEmbed(
                              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.already_registered.title"),
                              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.already_registered.message"),
                              BotHelper.MessageType.WARNING
                            )
                          )
                        );
                    } else {
                        replyCallback.editMessage(
                          BotHelper.buildEmbedMessage(true,
                            BotHelper.Feedback.buildEmbed(
                              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.locked.title"),
                              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.locked.message", BotHelper.formatDiscordTimestamp(whitelistedAccount.get().getProfile().getLockedUntil())),
                              BotHelper.MessageType.WARNING
                            )
                          )
                        );
                    }
                    return;
                }
            }

            String highestRole = roles.stream().map(Role::getId).filter(AutoWhitelist.ENTRY_MAP_CACHE::containsKey).findFirst().get();
            EntryData entry = AutoWhitelist.ENTRY_MAP_CACHE.get(highestRole);
            try {
                entry.assertSafe();
            } catch (Throwable e) {
                replyCallback.editMessage(
                    BotHelper.buildEmbedMessage(true,
                        BotHelper.Feedback.buildEmbed(
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.fail.title"),
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.fatal.exception", e.getMessage()),
                        BotHelper.MessageType.ERROR
                        )
                    )
                );
                AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to assert entry but got an exception", e);
                return;
            }

            if (username.isEmpty()) {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.few_args.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.few_args.message"),
                      BotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
            String[] args = username.split(" ");
            if (args.length > 1) {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.too_many_args.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.too_many_args.message"),
                      BotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
            String arg = args[0];

            {
                if (arg.length() > 16) {
                    replyCallback.editMessage(
                      BotHelper.buildEmbedMessage(true,
                        BotHelper.Feedback.buildEmbed(
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.invalid_username.title"),
                          /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.invalid_username.message.too_long"),
                          BotHelper.MessageType.WARNING
                        )
                      )
                    );
                    return;
                } else if (arg.length() < 3) {
                    replyCallback.editMessage(
                        BotHelper.buildEmbedMessage(true,
                            BotHelper.Feedback.buildEmbed(
                            /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.invalid_username.title"),
                            /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.invalid_username.message.too_short"),
                            BotHelper.MessageType.WARNING
                            )
                        )
                    );
                    return;
                }
            }
            GameProfile profile = server.getUserCache().findByName(arg).orElse(null);
            if (profile == null) {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.fail.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.fail.account_data", arg),
                      BotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole, id, AutoWhitelist.CONFIG.lockTime());
            if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.player_banned.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.player_banned.message"),
                      BotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }
            boolean whitelisted = whitelist.isAllowed(extendedProfile);
            if (whitelisted) {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.username_already_registered.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.username_already_registered.message"),
                      BotHelper.MessageType.ERROR
                    )
                  )
                );
            } else {
                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.last_steps.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.last_steps.message"),
                      BotHelper.MessageType.INFO
                    )
                  )
                );

                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.last_steps.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.last_steps.message"),
                      BotHelper.MessageType.INFO
                    )
                  )
                );

                if (whitelistedAccount.isPresent()) {
                    whitelist.remove(whitelistedAccount.get().getProfile());
                    if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                        AutoWhitelist.WHITELIST_CACHE.remove(new WhitelistCacheEntry(whitelistedAccount.get().getProfile()));
                    }
                }
                whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
                if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                    AutoWhitelist.WHITELIST_CACHE.add(new WhitelistCacheEntry(extendedProfile));
                }
                entry.registerUser(profile);

                replyCallback.editMessage(
                  BotHelper.buildEmbedMessage(true,
                    BotHelper.Feedback.buildEmbed(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.success.title"),
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.success.message"),
                      BotHelper.MessageType.SUCCESS
                    )
                  )
                );
            }
        } else {
            replyCallback.editMessage(
              BotHelper.buildEmbedMessage(true,
                BotHelper.Feedback.buildEmbed(
                  /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.fail.not_allowed.title"),
                  /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.register.fail.not_allowed.message"),
                  BotHelper.MessageType.NORMAL
                )
              )
            );
        }
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

            options.add(new OptionData(OptionType.STRING, "username", "Your Minecraft username").setRequired(true));
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            RegisterCommand.execute(event.getMember(), event.getOption("username").getAsString(), new ReplyCallback.InteractionReplyCallback() {

                @Override
                public void acknowledge() {
                    lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
                }
            });
        }
    }
}
