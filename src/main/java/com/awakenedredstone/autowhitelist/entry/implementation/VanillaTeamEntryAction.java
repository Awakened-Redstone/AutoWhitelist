package com.awakenedredstone.autowhitelist.entry.implementation;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

public class VanillaTeamEntryAction extends BaseEntryAction {
    public static final Identifier ID = AutoWhitelist.id("team");
    public static final MapCodec<VanillaTeamEntryAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntryAction::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntryAction::getType),
        Codec.STRING.fieldOf("associate_team").codec().fieldOf("execute").forGetter(team -> team.team)
      ).apply(instance, VanillaTeamEntryAction::new)
    );
    private final String team;

    public VanillaTeamEntryAction(List<String> roles, Identifier type, String team) {
        super(type, roles);
        this.team = team;
    }

    @Override
    public boolean isValid() {
        if (AutoWhitelist.getServer().getScoreboard().getTeam(team) == null) {
            LOGGER.error("The team \"{}\" does not exist!", team);
            return false;
        }
        return true;
    }

    @Override
    public void registerUser(LinkedPlayerProfile profile) {
        ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        Team serverTeam = scoreboard.getTeam(team);
        scoreboard.addScoreHolderToTeam(Stonecutter.profileName(profile), serverTeam);
    }

    @Override
    public void removeUser(LinkedPlayerProfile profile) {
        AutoWhitelist.getServer().getScoreboard().clearTeam(Stonecutter.profileName(profile));
    }

    @Override
    public String toString() {
        return "TeamEntry{" +
               "team='" + team + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntryAction otherEntry) {
        VanillaTeamEntryAction other = (VanillaTeamEntryAction) otherEntry;
        return Objects.equals(team, other.team);
    }
}
