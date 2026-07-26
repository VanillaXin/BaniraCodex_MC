package xin.vanilla.banira.internal.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraCommandService;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 内部命令门面；common 工具类只依赖这里，加载器差异交给具体实现。
 */
public final class BaniraCommandAccess {
    private static final BaniraCommandService SERVICE = new FabricBaniraCommandService();

    private BaniraCommandAccess() {
    }

    public static boolean executePlayerCommand(ServerPlayer player, String command, int permission, boolean suppressedOutput) {
        return SERVICE.executePlayerCommand(player, command, permission, suppressedOutput);
    }

    public static boolean hasPermission(Object source, int permission) {
        return SERVICE.hasPermission(source, permission);
    }

    public static Entity sourceEntity(Object source) {
        return SERVICE.sourceEntity(source);
    }

    public static ServerPlayer sourcePlayer(Object source) throws CommandSyntaxException {
        return SERVICE.sourcePlayer(source);
    }

    public static ServerLevel dimension(CommandContext<?> context, String name) throws CommandSyntaxException {
        return SERVICE.dimension(context, name);
    }

    public static ResourceKey<Level> dimensionKey(CommandContext<?> context, String name) throws CommandSyntaxException {
        return SERVICE.dimensionKey(context, name);
    }

    public static ServerPlayer player(CommandContext<?> context, String name) throws CommandSyntaxException {
        return SERVICE.player(context, name);
    }

    public static ServerPlayer playerOrSelf(CommandContext<?> context, String name) throws CommandSyntaxException {
        return SERVICE.playerOrSelf(context, name);
    }

    public static Collection<ServerPlayer> players(CommandContext<?> context, String name) throws CommandSyntaxException {
        return SERVICE.players(context, name);
    }

    public static void sendSuccess(Object source, Component message, boolean notifyAdmins) {
        SERVICE.sendSuccess(source, message, notifyAdmins);
    }

    public static void sendFailure(Object source, Component message) {
        SERVICE.sendFailure(source, message);
    }

    public static Object literal(String name) {
        return SERVICE.literal(name);
    }

    public static void executes(Object commandNode, BaniraCommandExecutor executor) {
        SERVICE.executes(commandNode, executor);
    }

    public static void then(Object parentNode, Object childNode) {
        SERVICE.then(parentNode, childNode);
    }

    public static void register(Object dispatcher, Object commandNode) {
        SERVICE.register(dispatcher, commandNode);
    }

    public static void onRegisterDispatcher(Consumer<Object> callback) {
        SERVICE.onRegisterDispatcher(callback);
    }

    public static void dispatchRegisterDispatcher(Object dispatcher) {
        SERVICE.dispatchRegisterDispatcher(dispatcher);
    }
}
