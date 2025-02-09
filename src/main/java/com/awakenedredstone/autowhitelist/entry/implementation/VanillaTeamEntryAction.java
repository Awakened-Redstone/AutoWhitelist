package com.awakenedredstone.autowhitelist.entry.implementation;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

public class VanillaTeamEntryAction extends BaseEntryAction {
    public static final Identifier ID = AutoWhitelist.id("team");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<VanillaTeamEntryAction> CODEC = Stonecutter.entryCodec(instance ->
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
    public void registerUser(ExtendedGameProfile profile) {
        ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        Team serverTeam = scoreboard.getTeam(team);
        /*? if >=1.20.3 {*/
        scoreboard.addScoreHolderToTeam(profile.getName(), serverTeam);
        /*?} else {*/
        /*scoreboard.addPlayerToTeam(profile.getName(), serverTeam);
        *//*?}*/
    }

    @Override
    public void removeUser(ExtendedGameProfile profile) {
        /*? if >=1.20.3 {*/
        AutoWhitelist.getServer().getScoreboard().clearTeam(profile.getName());
        /*?} else {*/
        /*AutoWhitelist.getServer().getScoreboard().clearPlayerTeam(profile.getName());
        *//*?}*/
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
