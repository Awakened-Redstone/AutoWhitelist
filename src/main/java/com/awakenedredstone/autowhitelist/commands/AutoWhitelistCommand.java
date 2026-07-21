package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.permission.CommandPermission;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCache;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCacheEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.data.DefaultTranslationsDataProvider;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.util.string.LinedStringBuilder;
import com.awakenedredstone.autowhitelist.util.data.ModData;
import com.awakenedredstone.autowhitelist.util.string.TimeParser;
import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandData;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// TODO: cleanup
public class AutoWhitelistCommand {
    public static final Logger LOGGER = LoggerFactory.getLogger(AutoWhitelistCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
          literal("autowhitelist")
            .requires(CommandPermission.admins("autowhitelist.command").check())
            .then(
              literal("dump")
                .then(
                  literal("stats")
                    .executes(AutoWhitelistCommand::getInfo)
                )
                .then(
                  literal("status")
                    .executes(AutoWhitelistCommand::getInfo)
                )
                .then(
                  literal("info")
                    .executes(AutoWhitelistCommand::getInfo)
                ).then(
                  literal("config")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal(AutoWhitelist.CONFIG.serialize()), false);
                        return 0;
                    })
                ).then(
                  literal("mods")
                    .executes(context -> {
                        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
                        mods.sort(Comparator.comparing(o -> o.getMetadata().getName()));
                        LinedStringBuilder builder = new LinedStringBuilder("Detected ", mods.size(), " mods:");

                        for (ModContainer mod : mods) {
                            ModMetadata modMeta = mod.getMetadata();
                            builder.appendLine(modMeta.getName()).append(" - ").append(modMeta.getVersion().getFriendlyString());
                        }

                        context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                        return mods.size();
                    })
                )
            ).then(
              literal("reload")
                .then(
                  literal("bot")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();

                        if (DiscordClientHolder.hasTask() && !DiscordClientHolder.isInitialized()) {
                            source.sendSystemMessage(Component.literal("Warning, it is unsafe to restart the bot before it completes initialization!").withStyle(ChatFormatting.RED));
                        }
                        source.sendSuccess(() -> Component.literal("Restarting discord client"), true);
                        DiscordClientHolder.queue();
                        DiscordClientHolder.getCurrent().shutdown();
                        return 0;
                    })
                ).then(
                  literal("config")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        source.sendSuccess(() -> Component.literal("Reloading configurations."), true);
                        if (!AutoWhitelist.CONFIG.tryLoad()) {
                            // TODO: translation
                            source.sendFailure(Component.literal("An error occurred while loading the config, check the logs for details"));
                        }
                        return 0;
                    })
                )
            ).then(
              literal("list")
                .executes(context -> executeList(context.getSource()))
            ).then(
              literal("create-translations-datapack")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    source.sendSuccess(() -> Component.literal("Creating datapack"), false);

                    Path path = source.getServer().getWorldPath(LevelResource.DATAPACK_DIR).resolve("autowhitelist_translations");
                    FabricPackOutput output = new FabricPackOutput(FabricLoader.getInstance().getModContainer(AutoWhitelist.MOD_ID).get(), path, true);
                    DataProvider provider = new DefaultTranslationsDataProvider(output);
                    provider.run(CachedOutput.NO_CACHE).whenComplete((o, throwable) -> {
                        try {
                            Files.writeString(path.resolve("pack.mcmeta"), "{\"pack\": {\"pack_format\": 34,\"description\": \"\"}}");
                        } catch (IOException e) {
                            source.sendFailure(Component.literal("Failed to create pack.mcmeta for \"autowhitelist_translations\""));
                            LOGGER.error("Failed to create pack.mcmeta for \"autowhitelist_translations\"", e);
                            return;
                        }

                        source.sendSuccess(() -> Component.literal("Created datapack autowhitelist_translations"), false);
                    });

                    return 0;
                })
            ).then(literal("rebuild-from-cache")
              .then(argument("run actions", BoolArgumentType.bool())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    boolean runActions = BoolArgumentType.getBool(context, "run actions");

                    LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
                    WhitelistCache cache = whitelist.getCache();

                    int entries = 0;
                    for (WhitelistCacheEntry entry : cache.getEntries()) {
                        if (whitelist.isWhiteListed(entry.getUser())) {
                            entries++;
                            whitelist.remove(entry.getUser());

                            if (!runActions) {
                                whitelist.add(new LinkedWhitelistEntry(entry.getUser().withLockedUntil(AutoWhitelist.config().whitelist.lockTime())));
                            }
                        }
                    }

                    return entries;
                })
              )
            ).then(
              literal("remove-all-guild-commands")
                .then(literal("i-understand-this-will-also-delete-commands-that-are-not-related-to-the-mod")
                  .executes(context -> {
                      GatewayDiscordClient client = DiscordClientHolder.getCurrent().getClient();
                      long applicationId = client.rest().getApplicationId().blockOptional().orElseThrow();

                      long guildId = AutoWhitelist.config().discord.guildId;
                      List<ApplicationCommandData> commands = client.rest().getApplicationService().getGuildApplicationCommands(applicationId, guildId).collectList().block();
                      for (ApplicationCommandData command : Objects.requireNonNull(commands)) {
                          client.rest().getApplicationService().deleteGuildApplicationCommand(applicationId, guildId, command.id().asLong()).subscribe();
                      }

                      context.getSource().sendSuccess(() -> Component.literal("All commands have been removed, please restart the bot to add the required ones back. Some users may have to reload Discord to see the changes"), true);
                      return 0;
                  })
                )
            )
        );
    }

    public static int executeList(CommandSourceStack source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendSystemMessage(Component.literal("Loading info..."), true);
        }

        Collection<? extends UserWhiteListEntry> entries = source.getServer().getPlayerList().getWhiteList().getEntries();

        List</*$ WhitelistProfile {*/net.minecraft.server.players.NameAndId/*$}*/> profiles = entries.stream()
          .map(StoredUserEntry::getUser)
          .filter(profile -> !(profile instanceof LinkedPlayerProfile))
          .toList();

        MutableComponent list = Component.literal("");
        if (!profiles.isEmpty()) {
            list.append("Vanilla whitelist:");
            profiles.forEach(player -> list.append("\n").append("    ").append(PlayerProfile.name(player)));
        }

        List<LinkedPlayerProfile> extendedProfiles = entries.stream()
          .map(entry -> entry.getUser() instanceof LinkedPlayerProfile profile ? profile : null)
          .filter(Objects::nonNull)
          .toList();

        if (!extendedProfiles.isEmpty()) {
            if (!list.getString().isEmpty()) list.append("\n");
            list.append("Automated whitelist:");
            Guild guild = DiscordClientHolder.getCurrent().getGuild();
            if (guild != null) {
                for (LinkedPlayerProfile profile : extendedProfiles) {
                    list.append("\n").append("    ").append(PlayerProfile.name(profile));
                    list.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY));

                    Optional<Member> member = guild.getMemberById(Snowflake.of(profile.getDiscordId())).onErrorComplete().blockOptional();
                    if (member.isEmpty()) {
                        list.append(Component.literal("Invalid member").withStyle(ChatFormatting.RED));
                    } else {
                        list.append(Component.literal(member.get().getUsername()).withStyle(ChatFormatting.GRAY));
                    }

                    list.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
                    Optional<Role> role = guild.getRoleById(Snowflake.of(profile.getRole())).blockOptional();
                    if (role.isEmpty()) {
                        list.append(Component.literal("Invalid role").withStyle(ChatFormatting.RED));
                    } else {
                        list.append(Component.literal("@" + role.get().getName()).withStyle(ChatFormatting.GRAY));
                    }
                    list.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
                }
            } else {
                list.append("\n").append("    ").append(Component.literal("Failed to get guild!").withStyle(ChatFormatting.RED));
            }
        }

        if (source.getPlayer() != null) {
            source.getPlayer().sendSystemMessage(Component.literal(""), true);
        }

        source.sendSuccess(() -> list, false);
        return extendedProfiles.size();
    }

    private static int getInfo(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Generating data dump..."), false);
        PlayerList playerManager = ServerDetails.getServer().getPlayerList();

        CompletableFuture.runAsync(() -> {
            DataResult<AutoWhitelistConfig> configLoad = AutoWhitelist.CONFIG.tryRead();
            LinedStringBuilder dump = new LinedStringBuilder().append(" ");
            dump.appendLine("==== AutoWhitelist data dump ====");
            dump.appendLine("Minecraft:");
            dump.appendLine("  Minecraft version: ", SharedConstants.getCurrentVersion()./*? if <=1.21.5 {*//*getName*//*?} else {*/name/*?}*/());
            dump.appendLine("  Java version: ", Runtime.version());
            dump.appendLine("  Mod loader: ", ServerDetails.getPlatformName());
            if (FabricLoader.getInstance().isModLoaded("connectormod")) {
                dump.appendLine("  Connector version: ", ModData.getVersion("connectormod"));
            }
            dump.appendLine("  Loader version: ", ServerDetails.getLoaderVersion());
            dump.appendLine("  Mod version: ", ModData.getVersion("autowhitelist"));
            dump.appendLine("  Total whitelisted players: ", playerManager.getWhiteListNames().length);
            dump.appendLine("  Luckperms version: ", ModData.getVersion("luckperms"));

            dump.appendLine();
            dump.appendLine("AutoWhitelist:");
            dump.appendLine("  Config:");
            dump.appendLine("    Total entries: ", AutoWhitelist.config().whitelist.allow.size());
            dump.appendLine("    Config exists: ", AutoWhitelist.CONFIG.configExists());
            dump.appendLine("    Config status: ", configLoad.error().map(DataResult.Error::message).orElse("Loaded"));
            dump.appendLine("    Lock time: ", TimeParser.parseTime(AutoWhitelist.config().whitelist.lockTime));
            dump.appendLine("  Bot:");
            dump.appendLine("    Bot status: ", DiscordClientHolder.status().name().toLowerCase());

            context.getSource().sendSuccess(() -> Component.literal(dump.toString()), false);
        });

        return 0;
    }
}
