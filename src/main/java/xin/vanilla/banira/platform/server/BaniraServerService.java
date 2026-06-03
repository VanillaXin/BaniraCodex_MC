package xin.vanilla.banira.platform.server;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

import java.nio.file.Path;
import java.util.Collection;
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

    /**
     * 向玩家发送普通系统/聊天消息；各版本的 sendMessage/sendSystemMessage 差异由 adapter 处理。
     */
    void sendPlayerMessage(PlayerEntity player, ITextComponent message);

    /**
     * 向服务端玩家发送 actionbar 消息；packet 类型和 ChatType 差异由 adapter 处理。
     */
    void sendActionBarMessage(ServerPlayerEntity player, ITextComponent message);

    void refreshPlayerPermission(ServerPlayerEntity player);

    Collection<Advancement> advancements();

    IResourceManager serverResourceManager();

    Path worldDataPath(String directoryName);

    Path worldPlayerDataPath();

    long tickCount();
}
