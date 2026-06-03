package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.platform.server.BaniraServerService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Forge 1.16.5 server/player access. Player-list API churn stays in this adapter.
 */
public final class ForgeBaniraServerService implements BaniraServerService {
    @Override
    public MinecraftServer currentServer() {
        return BaniraCodex.serverInstance().key();
    }

    @Override
    public List<ServerPlayerEntity> players() {
        MinecraftServer server = currentServer();
        return server != null ? server.getPlayerList().getPlayers() : Collections.emptyList();
    }

    @Override
    public ServerPlayerEntity player(UUID uuid) {
        MinecraftServer server = currentServer();
        return server != null && uuid != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    @Override
    public void broadcastRawPacket(IPacket<?> packet) {
        if (packet == null) return;
        players().forEach(player -> player.connection.send(packet));
    }

    @Override
    public void broadcastSystemMessage(MinecraftServer server, ITextComponent message) {
        MinecraftServer target = server != null ? server : currentServer();
        if (target != null && message != null) {
            target.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }

    @Override
    public void refreshPlayerPermission(ServerPlayerEntity player) {
        MinecraftServer server = player != null ? player.getServer() : currentServer();
        if (server != null && player != null) {
            server.getPlayerList().sendPlayerPermissionLevel(player);
        }
    }

    @Override
    public Collection<Advancement> advancements() {
        MinecraftServer server = currentServer();
        return server != null ? server.getAdvancements().getAllAdvancements() : Collections.emptyList();
    }

    @Override
    public IResourceManager serverResourceManager() {
        MinecraftServer server = currentServer();
        return server != null ? server.getDataPackRegistries().getResourceManager() : null;
    }
}
