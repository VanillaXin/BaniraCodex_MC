package xin.vanilla.banira.internal.forge.platform;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.platform.command.BaniraCommandExecutor;
import xin.vanilla.banira.platform.command.BaniraCommandService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Forge 1.16.5 command-source and command-argument adapter.
 */
public final class ForgeBaniraCommandService implements BaniraCommandService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<Consumer<Object>> dispatcherCallbacks = new ArrayList<>();

    @Override
    public boolean executePlayerCommand(ServerPlayerEntity player, String command, int permission, boolean suppressedOutput) {
        try {
            MinecraftServer server = player != null ? player.getServer() : null;
            if (server == null || command == null) return false;
            CommandSource source = player.createCommandSourceStack();
            if (permission > 0) {
                source = source.withPermission(permission);
            }
            if (suppressedOutput) {
                source = source.withSuppressedOutput();
            }
            return server.getCommands().performCommand(source, command) > 0;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Object source, int permission) {
        if (source instanceof CommandSource) {
            return ((CommandSource) source).hasPermission(permission);
        }
        if (source instanceof ServerPlayerEntity) {
            return ((ServerPlayerEntity) source).createCommandSourceStack().hasPermission(permission);
        }
        return false;
    }

    @Override
    public Entity sourceEntity(Object source) {
        return source instanceof CommandSource ? ((CommandSource) source).getEntity() : null;
    }

    @Override
    public ServerPlayerEntity sourcePlayer(Object source) throws CommandSyntaxException {
        if (source instanceof CommandSource) {
            return ((CommandSource) source).getPlayerOrException();
        }
        throw CommandSource.ERROR_NOT_PLAYER.create();
    }

    @Override
    public ServerWorld dimension(CommandContext<?> context, String name) throws CommandSyntaxException {
        return DimensionArgument.getDimension(castContext(context), name);
    }

    @Override
    public RegistryKey<World> dimensionKey(CommandContext<?> context, String name) throws CommandSyntaxException {
        return dimension(context, name).dimension();
    }

    @Override
    public ServerPlayerEntity player(CommandContext<?> context, String name) throws CommandSyntaxException {
        return EntityArgument.getPlayer(castContext(context), name);
    }

    @Override
    public ServerPlayerEntity playerOrSelf(CommandContext<?> context, String name) throws CommandSyntaxException {
        try {
            return player(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return sourcePlayer(context.getSource());
        }
    }

    @Override
    public Collection<ServerPlayerEntity> players(CommandContext<?> context, String name) throws CommandSyntaxException {
        return EntityArgument.getPlayers(castContext(context), name);
    }

    @Override
    public void sendSuccess(Object source, ITextComponent message, boolean notifyAdmins) {
        if (source instanceof CommandSource && message != null) {
            ((CommandSource) source).sendSuccess(message, notifyAdmins);
        }
    }

    @Override
    public void sendFailure(Object source, ITextComponent message) {
        if (source instanceof CommandSource && message != null) {
            ((CommandSource) source).sendFailure(message);
        }
    }

    @Override
    public Object literal(String name) {
        return Commands.literal(name);
    }

    @Override
    public void executes(Object commandNode, BaniraCommandExecutor executor) {
        if (commandNode instanceof LiteralArgumentBuilder && executor != null) {
            ((LiteralArgumentBuilder<CommandSource>) commandNode).executes(context -> executor.run(context));
        }
    }

    @Override
    public void then(Object parentNode, Object childNode) {
        if (parentNode instanceof LiteralArgumentBuilder && childNode instanceof LiteralArgumentBuilder) {
            ((LiteralArgumentBuilder<CommandSource>) parentNode).then((LiteralArgumentBuilder<CommandSource>) childNode);
        }
    }

    @Override
    public void register(Object dispatcher, Object commandNode) {
        if (dispatcher instanceof CommandDispatcher && commandNode instanceof LiteralArgumentBuilder) {
            ((CommandDispatcher<CommandSource>) dispatcher).register((LiteralArgumentBuilder<CommandSource>) commandNode);
        }
    }

    @Override
    public void onRegisterDispatcher(Consumer<Object> callback) {
        if (callback != null) {
            dispatcherCallbacks.add(callback);
        }
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
    private CommandContext<CommandSource> castContext(CommandContext<?> context) {
        return (CommandContext<CommandSource>) context;
    }
}
