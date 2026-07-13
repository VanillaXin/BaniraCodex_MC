package xin.vanilla.banira.internal.server;

import net.minecraft.advancements.Advancement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraServerService;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Internal server facade. Public platform API stays small while version-specific server access remains replaceable.
 */
public final class BaniraServerAccess {
    private static final BaniraServerService SERVICE = new FabricBaniraServerService();

    private BaniraServerAccess() {
    }

    public static MinecraftServer currentServer() {
        return SERVICE.currentServer();
    }

    public static List<ServerPlayer> players() {
        return SERVICE.players();
    }

    public static ServerPlayer player(UUID uuid) {
        return SERVICE.player(uuid);
    }

    public static void broadcastRawPacket(Packet<?> packet) {
        SERVICE.broadcastRawPacket(packet);
    }

    public static void broadcastSystemMessage(MinecraftServer server, Component message) {
        SERVICE.broadcastSystemMessage(server, message);
    }

    public static void sendPlayerMessage(Player player, Component message) {
        SERVICE.sendPlayerMessage(player, message);
    }

    public static void sendActionBarMessage(ServerPlayer player, Component message) {
        SERVICE.sendActionBarMessage(player, message);
    }

    public static void refreshPlayerPermission(ServerPlayer player) {
        SERVICE.refreshPlayerPermission(player);
    }

    public static Collection<Advancement> advancements() {
        return SERVICE.advancements();
    }

    public static ResourceManager serverResourceManager() {
        return SERVICE.serverResourceManager();
    }

    public static Path worldDataPath(String directoryName) {
        return SERVICE.worldDataPath(directoryName);
    }

    public static Path worldPlayerDataPath() {
        return SERVICE.worldPlayerDataPath();
    }

    public static long tickCount() {
        return SERVICE.tickCount();
    }
}
