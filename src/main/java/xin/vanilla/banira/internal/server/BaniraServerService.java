package xin.vanilla.banira.internal.server;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Loader/version-neutral access to the active logical server and its players.
 */
public interface BaniraServerService {
    MinecraftServer currentServer();

    List<ServerPlayer> players();

    ServerPlayer player(UUID uuid);

    void broadcastRawPacket(Packet<?> packet);

    void broadcastSystemMessage(MinecraftServer server, Component message);

    /**
     * 向玩家发送普通系统/聊天消息；各版本的 sendMessage/sendSystemMessage 差异由 adapter 处理。
     */
    void sendPlayerMessage(Player player, Component message);

    /**
     * 向服务端玩家发送 actionbar 消息；packet 类型和 ChatType 差异由 adapter 处理。
     */
    void sendActionBarMessage(ServerPlayer player, Component message);

    void refreshPlayerPermission(ServerPlayer player);

    Collection<Advancement> advancements();

    ResourceManager serverResourceManager();

    Path worldDataPath(String directoryName);

    Path worldPlayerDataPath();

    long tickCount();
}
