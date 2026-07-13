package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.internal.server.BaniraServerService;
import xin.vanilla.banira.internal.mixin.accessors.MinecraftServerAccessor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Fabric 1.16 服务端访问适配。 */
public final class FabricBaniraServerService implements BaniraServerService,
        xin.vanilla.banira.platform.BaniraServerService {
    public static final FabricBaniraServerService INSTANCE = new FabricBaniraServerService();

    @Override
    public Object current() {
        return currentServer();
    }

    @Override
    public boolean isRunning() {
        return BaniraCodex.serverInstance().val();
    }

    @Override
    public MinecraftServer currentServer() { return BaniraCodex.serverInstance().key(); }

    @Override
    public List<ServerPlayer> players() {
        MinecraftServer server = currentServer();
        return server != null ? server.getPlayerList().getPlayers() : Collections.emptyList();
    }

    @Override
    public ServerPlayer player(UUID uuid) {
        MinecraftServer server = currentServer();
        return server != null && uuid != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    @Override
    public void broadcastRawPacket(Packet<?> packet) {
        if (packet != null) players().forEach(player -> player.connection.send(packet));
    }

    @Override
    public void broadcastSystemMessage(MinecraftServer server, Component message) {
        MinecraftServer target = server != null ? server : currentServer();
        if (target != null && message != null) target.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, Util.NIL_UUID);
    }

    @Override
    public void sendPlayerMessage(Player player, Component message) {
        if (player != null && message != null) player.sendMessage(message, player.getUUID());
    }

    @Override
    public void sendActionBarMessage(ServerPlayer player, Component message) {
        if (player != null && message != null) player.connection.send(new ClientboundChatPacket(message, ChatType.GAME_INFO, player.getUUID()));
    }

    @Override
    public void refreshPlayerPermission(ServerPlayer player) {
        MinecraftServer server = player != null ? player.getServer() : currentServer();
        if (server != null && player != null) server.getPlayerList().sendPlayerPermissionLevel(player);
    }

    @Override
    public Collection<Advancement> advancements() {
        MinecraftServer server = currentServer();
        return server != null ? server.getAdvancements().getAllAdvancements() : Collections.emptyList();
    }

    @Override
    public ResourceManager serverResourceManager() {
        MinecraftServer server = currentServer();
        return server != null ? ((MinecraftServerAccessor) server).banira$resources().getResourceManager() : null;
    }

    @Override
    public Path worldDataPath(String directoryName) {
        MinecraftServer server = currentServer();
        return server != null && directoryName != null ? server.getWorldPath(LevelResource.ROOT).resolve(directoryName) : null;
    }

    @Override
    public Path worldPlayerDataPath() {
        MinecraftServer server = currentServer();
        return server != null ? server.getWorldPath(LevelResource.PLAYER_DATA_DIR) : null;
    }

    @Override
    public long tickCount() {
        MinecraftServer server = currentServer();
        return server != null ? server.getTickCount() : 0;
    }
}
