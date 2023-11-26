package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.api.Permission;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.LinedStringBuilder;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.dv8tion.jda.api.JDAInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;
import static net.minecraft.server.command.CommandManager.literal;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("autowhitelist")
            .requires(Permission.require("autowhitelist.command", 3))
            .then(literal("dump")
              .executes(context -> {
                  context.getSource().sendFeedback(() -> Text.literal("AutoWhitelist data dump..."), false);
                  PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();

                  LinedStringBuilder dump = new LinedStringBuilder();
                  dump.appendLine("Server version: ", SharedConstants.getGameVersion().getName());
                  dump.appendLine("Mod loader: ", AutoWhitelist.getServer().getServerModName());
                  if (ModData.isModLoaded("fabricloader")) {
                      dump.appendLine("Fabric loader: ", ModData.getVersion("fabricloader"));
                  }
                  if (ModData.isModLoaded("quilt_loader")) {
                      dump.appendLine("Quilt loader: ", ModData.getVersion("quilt_loader"));
                  }
                  dump.appendLine("Mod version: ", ModData.getVersion("autowhitelist"));
                  dump.appendLine("Total whitelisted players: ", playerManager.getWhitelistedNames().length);
                  dump.appendLine("Luckperms versions: ", ModData.getVersion("luckperms"));
                  dump.appendLine("JDA version: ", JDAInfo.VERSION);
                  Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream().filter(mod -> !isCoreMod(mod.getMetadata().getId())).toList();
                  dump.appendLine("Found ", mods.size(), " other non \"core\" mods");
                  dump.appendLine("Total entries: ", AutoWhitelist.CONFIG.entries.size());
                  dump.appendLine("Config exists: ", AutoWhitelist.CONFIG.configExists());
                  dump.appendLine("Config loaded: ", AutoWhitelist.CONFIG.tryLoad());

                  context.getSource().sendFeedback(() -> Text.literal(dump.toString()), false);
                  return 0;
              }).then(literal("config")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal(AutoWhitelist.CONFIG.toString()), false);
                    return 0;
                })
              )
            ).then(literal("reload")
                .executes(context -> executeReload(context.getSource()))
                .then(literal("bot")
                    .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.BOT))
                ).then(literal("config")
                    .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.CONFIG))
                ).then(literal("cache")
                    .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.CACHE))
                )
            ).then(literal("entries")
                .executes(context -> executeEntries(context.getSource()))
            ).then(literal("info")
                .executes(context -> executeInfo(context.getSource()))
            )
        );
    }

    public static int executeInfo(ServerCommandSource source) {
        String text = "Mod information:" +
            "    Bot: " + (Bot.jda == null ? "offline" : "online");
        return Bot.jda != null ? 1 : 0;
    }

    public static int executeEntries(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("Loading info..."), true);
        }

        Collection<? extends WhitelistEntry> entries = ((ExtendedWhitelist) source.getServer().getPlayerManager().getWhitelist()).getEntries();

        Stream<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey());

        StringBuilder list = new StringBuilder();
        list.append("Non-managed players:\n");
        profiles.filter(profile -> !(profile instanceof ExtendedGameProfile)).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey());

        list.append("\n");
        list.append("Managed players:\n");
        profiles.filter(profile -> profile instanceof ExtendedGameProfile).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal(""), true);
        }

        source.sendFeedback(() -> Text.literal(list.toString()), false);
        return 0;
    }

    public static int executeReload(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Reloading AutoWhitelist configurations, please wait."), true);

        analyzeTimings("Configs#load", AutoWhitelist.CONFIG::load);
        analyzeTimings("AutoWhitelist#loadWhitelistCache", AutoWhitelist::loadWhitelistCache);
        source.sendFeedback(() -> Text.literal("Restarting bot, please wait."), true);
        analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));

        return 0;
    }

    public static int executeSpecificReload(ServerCommandSource source, ReloadableObjects type) {
        switch (type) {
            case BOT -> {
                source.sendFeedback(() -> Text.literal("Restarting bot, please wait."), true);
                analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));
            }
            case CONFIG -> {
                source.sendFeedback(() -> Text.literal("Reloading configurations."), true);
                analyzeTimings("Configs#load", AutoWhitelist.CONFIG::load);
            }
            case CACHE -> {
                source.sendFeedback(() -> Text.literal("Reloading cache."), true);
                analyzeTimings("AutoWhitelist#loadWhitelistCache", AutoWhitelist::loadWhitelistCache);
            }
        }

        return 0;
    }

    private static boolean isCoreMod(String modid) {
        return Pattern.compile("^fabric(-\\w+)+-v\\d$").matcher(modid).matches() || equals(modid, "java", "minecraft", "fabricloader", "autowhitelist",
          "placeholder-api", "server_translations_api", "packet_tweaker", "fabric-language-kotlin", "fabric-api", "fabric-api-base");
    }

    private static boolean equals(String stringA, String... stringB) {
        //return true if stringA equals to any of stringB
        for (String string : stringB) {
            if (stringA.equals(string)) return true;
        }
        return false;
    }

    private enum ReloadableObjects {
        BOT,
        CONFIG,
        CACHE
    }
}
