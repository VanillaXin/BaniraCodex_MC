package xin.vanilla.banira.internal.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Banira 内部命令适配接口；不同版本的参数和注册 API 只在内部实现中变化。
 */
public interface BaniraCommandService {
    boolean executePlayerCommand(ServerPlayer player, String command, int permission, boolean suppressedOutput);

    boolean hasPermission(Object source, int permission);

    Entity sourceEntity(Object source);

    ServerPlayer sourcePlayer(Object source) throws CommandSyntaxException;

    ServerLevel dimension(CommandContext<?> context, String name) throws CommandSyntaxException;

    ResourceKey<Level> dimensionKey(CommandContext<?> context, String name) throws CommandSyntaxException;

    ServerPlayer player(CommandContext<?> context, String name) throws CommandSyntaxException;

    ServerPlayer playerOrSelf(CommandContext<?> context, String name) throws CommandSyntaxException;

    Collection<ServerPlayer> players(CommandContext<?> context, String name) throws CommandSyntaxException;

    void sendSuccess(Object source, Component message, boolean notifyAdmins);

    void sendFailure(Object source, Component message);

    Object literal(String name);

    void executes(Object commandNode, BaniraCommandExecutor executor);

    void then(Object parentNode, Object childNode);

    void register(Object dispatcher, Object commandNode);

    void onRegisterDispatcher(Consumer<Object> callback);

    void dispatchRegisterDispatcher(Object dispatcher);
}
