package com.awakenedredstone.autowhitelist.mixin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Whitelist;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    public ClientConnection connection;
    @Shadow
    @Nullable GameProfile profile;
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "acceptPlayer", at = @At("HEAD"))
    private void autoWhitelist$checkCache(CallbackInfo ci) {
        if (!AutoWhitelist.CONFIG.enableWhitelistCache()) return;
        if (Bot.jda == null) return;
        if (Bot.guild == null) return;
        if (this.profile == null) return;
        if (this.server.getPlayerManager().checkCanJoin(this.connection.getAddress(), this.profile) == null) return;
        if (this.profile.isComplete()) {
            WhitelistCacheEntry cachedEntry = AutoWhitelist.WHITELIST_CACHE.get(this.profile);
            if (cachedEntry == null) return;
            String discordId = cachedEntry.getProfile().getDiscordId();
            Member member = Bot.guild.getMemberById(discordId);
            if (member == null) {
                AutoWhitelist.WHITELIST_CACHE.remove(this.profile);
                return;
            }
            List<Role> roles = member.getRoles();

            Optional<String> roleOptional = getTopRole(roles);
            if (roleOptional.isEmpty()) return;
            String role = roleOptional.get();

            EntryData entry = AutoWhitelist.whitelistDataMap.get(role);
            if (hasException(entry::assertSafe)) return;

            Whitelist whitelist = server.getPlayerManager().getWhitelist();
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), role, discordId);
            whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
            entry.registerUser(profile);
        }
    }

    @Unique
    private Optional<String> getTopRole(List<Role> roles) {
        for (Role r : roles)
            if (AutoWhitelist.whitelistDataMap.containsKey(r.getId())) return Optional.of(r.getId());

        return Optional.empty();
    }

    @Unique
    private boolean hasException(Runnable task) {
        try {
            task.run();
            return false;
        } catch (Throwable e) {
            LOGGER.error("Failed to use whitelist cache due to a broken entry, please check your config file!", e);
            return true;
        }
    }
}
