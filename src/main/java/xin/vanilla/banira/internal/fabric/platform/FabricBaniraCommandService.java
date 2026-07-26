package xin.vanilla.banira.internal.fabric.platform;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.internal.command.BaniraCommandExecutor;
import xin.vanilla.banira.internal.command.BaniraCommandService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.16 Brigadier 命令适配；命令语义不依赖具体加载器。
 */
public final class FabricBaniraCommandService implements BaniraCommandService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<Consumer<Object>> dispatcherCallbacks = new ArrayList<>();

    @Override
    public boolean executePlayerCommand(ServerPlayer player, String command, int permission, boolean suppressedOutput) {
        try {
            MinecraftServer server = player != null ? player.getServer() : null;
            if (server == null || command == null) return false;
            CommandSourceStack source = player.createCommandSourceStack();
            if (permission > 0) source = source.withPermission(permission);
            if (suppressedOutput) source = source.withSuppressedOutput();
            return server.getCommands().performCommand(source, command) > 0;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Object source, int permission) {
        if (source instanceof CommandSourceStack) return ((CommandSourceStack) source).hasPermission(permission);
        if (source instanceof ServerPlayer)
            return ((ServerPlayer) source).createCommandSourceStack().hasPermission(permission);
        return false;
    }

    @Override
    public Entity sourceEntity(Object source) {
        return source instanceof CommandSourceStack ? ((CommandSourceStack) source).getEntity() : null;
    }

    @Override
    public ServerPlayer sourcePlayer(Object source) throws CommandSyntaxException {
        if (source instanceof CommandSourceStack) return ((CommandSourceStack) source).getPlayerOrException();
        throw CommandSourceStack.ERROR_NOT_PLAYER.create();
    }

    @Override
    public ServerLevel dimension(CommandContext<?> context, String name) throws CommandSyntaxException {
        return DimensionArgument.getDimension(cast(context), name);
    }

    @Override
    public ResourceKey<Level> dimensionKey(CommandContext<?> context, String name) throws CommandSyntaxException {
        return dimension(context, name).dimension();
    }

    @Override
    public ServerPlayer player(CommandContext<?> context, String name) throws CommandSyntaxException {
        return EntityArgument.getPlayer(cast(context), name);
    }

    @Override
    public ServerPlayer playerOrSelf(CommandContext<?> context, String name) throws CommandSyntaxException {
        try {
            return player(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return sourcePlayer(context.getSource());
        }
    }

    @Override
    public Collection<ServerPlayer> players(CommandContext<?> context, String name) throws CommandSyntaxException {
        return EntityArgument.getPlayers(cast(context), name);
    }

    @Override
    public void sendSuccess(Object source, Component message, boolean notifyAdmins) {
        if (source instanceof CommandSourceStack && message != null)
            ((CommandSourceStack) source).sendSuccess(message, notifyAdmins);
    }

    @Override
    public void sendFailure(Object source, Component message) {
        if (source instanceof CommandSourceStack && message != null) ((CommandSourceStack) source).sendFailure(message);
    }

    @Override
    public Object literal(String name) {
        return Commands.literal(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executes(Object commandNode, BaniraCommandExecutor executor) {
        if (commandNode instanceof LiteralArgumentBuilder && executor != null)
            ((LiteralArgumentBuilder<CommandSourceStack>) commandNode).executes(executor::run);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void then(Object parentNode, Object childNode) {
        if (parentNode instanceof LiteralArgumentBuilder && childNode instanceof LiteralArgumentBuilder)
            ((LiteralArgumentBuilder<CommandSourceStack>) parentNode).then((LiteralArgumentBuilder<CommandSourceStack>) childNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(Object dispatcher, Object commandNode) {
        if (dispatcher instanceof CommandDispatcher && commandNode instanceof LiteralArgumentBuilder)
            ((CommandDispatcher<CommandSourceStack>) dispatcher).register((LiteralArgumentBuilder<CommandSourceStack>) commandNode);
    }

    @Override
    public void onRegisterDispatcher(Consumer<Object> callback) {
        if (callback != null) dispatcherCallbacks.add(callback);
    }

    @Override
    public void dispatchRegisterDispatcher(Object dispatcher) {
        for (Consumer<Object> callback : dispatcherCallbacks) {
            try {
                callback.accept(dispatcher);
            } catch (Throwable t) {
                LOGGER.warn("Error executing command dispatcher registration callback", t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandContext<CommandSourceStack> cast(CommandContext<?> context) {
        return (CommandContext<CommandSourceStack>) context;
    }
}
