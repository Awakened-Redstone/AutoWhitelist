package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

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
    public void assertValid() {
        if (AutoWhitelist.getServer().getScoreboard().getTeam(team) == null) {
            throw new AssertionError(String.format("The team \"%s\" does not exist!", team));
        }
    }

    @Override
    public <T extends GameProfile> void registerUser(T profile) {
        ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
        Team serverTeam = scoreboard.getTeam(team);
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
    public String toString() {
        return "TeamEntry{" +
               "team='" + team + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntry otherEntry) {
        TeamEntry other = (TeamEntry) otherEntry;
        return Objects.equals(team, other.team);
    }
}
