package xin.vanilla.banira.internal.server;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraServerService;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Internal server facade. Public platform API stays small while version-specific server access remains replaceable.
 */
public final class BaniraServerAccess {
    private static final BaniraServerService SERVICE = new ForgeBaniraServerService();

    private BaniraServerAccess() {
    }

    public static MinecraftServer currentServer() {
        return SERVICE.currentServer();
    }

    public static List<ServerPlayerEntity> players() {
        return SERVICE.players();
    }

    public static ServerPlayerEntity player(UUID uuid) {
        return SERVICE.player(uuid);
    }

    public static void broadcastRawPacket(IPacket<?> packet) {
        SERVICE.broadcastRawPacket(packet);
    }

    public static void broadcastSystemMessage(MinecraftServer server, ITextComponent message) {
        SERVICE.broadcastSystemMessage(server, message);
    }

    public static void sendPlayerMessage(PlayerEntity player, ITextComponent message) {
        SERVICE.sendPlayerMessage(player, message);
    }

    public static void sendActionBarMessage(ServerPlayerEntity player, ITextComponent message) {
        SERVICE.sendActionBarMessage(player, message);
    }

    public static void refreshPlayerPermission(ServerPlayerEntity player) {
        SERVICE.refreshPlayerPermission(player);
    }

    public static Collection<Advancement> advancements() {
        return SERVICE.advancements();
    }

    public static IResourceManager serverResourceManager() {
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
