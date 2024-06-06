package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class TeamEntry extends BaseEntry {
    public static final Identifier ID = AutoWhitelist.id("team");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<TeamEntry> CODEC = Stonecutter.entryCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntry::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntry::getType),
        Codec.STRING.fieldOf("associate_team").codec().fieldOf("execute").forGetter(team -> team.team)
      ).apply(instance, TeamEntry::new)
    );
    private final String team;

    public TeamEntry(List<String> roles, Identifier type, String team) {
        super(type, roles);
        this.team = team;
    }

    @Override
    public void assertSafe() {
        if (AutoWhitelist.getServer().getScoreboard().getTeam(team) == null) {
            throw new AssertionError(String.format("The team \"%s\" does not exist!", team));
        }
    }

    @Override
    public void purgeInvalid() {
        Scoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        net.minecraft.scoreboard.Team serverTeam = scoreboard.getTeam(team);
        if (serverTeam == null) {
            AutoWhitelist.LOGGER.error("Could not check for invalid players on team \"{}\", could not find the team", team);
            return;
        }
        PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) playerManager.getWhitelist();
        Collection<? extends net.minecraft.server.WhitelistEntry> entries = whitelist.getEntries();
        List<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey()).toList();
        List<String> invalidPlayers = serverTeam.getPlayerList().stream().filter(player -> {
            GameProfile profile = profiles.stream().filter(v -> v.getName().equals(player)).findFirst().orElse(null);
            if (profile == null) return true;
            return !whitelist.isAllowed(profile);
        }).toList();
        /*? if >=1.20.3 {*/
        invalidPlayers.forEach(player -> scoreboard.removeScoreHolderFromTeam(player, serverTeam));
        /*?} else {*/
            /*invalidPlayers.forEach(player -> scoreboard.removePlayerFromTeam(player, serverTeam));
            *//*?}*/
    }

    @Override
    public <T extends GameProfile> void registerUser(T profile) {
        ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        net.minecraft.scoreboard.Team serverTeam = scoreboard.getTeam(team);
        /*? if >=1.20.3 {*/
        scoreboard.addScoreHolderToTeam(profile.getName(), serverTeam);
        /*?} else {*/
            /*scoreboard.addPlayerToTeam(profile.getName(), serverTeam);
            *//*?}*/
    }

    @Override
    public <T extends GameProfile> void removeUser(T profile) {
        /*? if >=1.20.3 {*/
        AutoWhitelist.getServer().getScoreboard().clearTeam(profile.getName());
        /*?} else {*/
            /*AutoWhitelist.getServer().getScoreboard().clearPlayerTeam(profile.getName());
            *//*?}*/
    }

    @Override
    public <T extends GameProfile> void updateUser(T profile, @Nullable BaseEntry oldEntry) {
        registerUser(profile);
    }

    @Override
    public <T extends GameProfile> boolean shouldUpdate(T profile) {
        ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        net.minecraft.scoreboard.Team serverTeam = scoreboard.getTeam(team);

        /*? if >=1.20.3 {*/
        return scoreboard.getScoreHolderTeam(profile.getName()) != serverTeam;
        /*?} else {*/
            /*return scoreboard.getPlayerTeam(profile.getName()) != serverTeam;
            *//*?}*/
    }
}
