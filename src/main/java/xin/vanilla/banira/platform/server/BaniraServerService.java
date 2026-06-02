package xin.vanilla.banira.platform.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.UUID;

/**
 * Loader/version-neutral access to the active logical server and its players.
 */
public interface BaniraServerService {
    MinecraftServer currentServer();

    List<ServerPlayerEntity> players();

    ServerPlayerEntity player(UUID uuid);

    void broadcastRawPacket(IPacket<?> packet);

    void broadcastSystemMessage(MinecraftServer server, ITextComponent message);

    void refreshPlayerPermission(ServerPlayerEntity player);
}
