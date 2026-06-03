package xin.vanilla.banira.platform.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Loader/version-neutral command helpers for vanilla command source and argument APIs.
 */
public interface BaniraCommandService {
    boolean executePlayerCommand(ServerPlayerEntity player, String command, int permission, boolean suppressedOutput);

    boolean hasPermission(Object source, int permission);

    Entity sourceEntity(Object source);

    ServerPlayerEntity sourcePlayer(Object source) throws CommandSyntaxException;

    ServerWorld dimension(CommandContext<?> context, String name) throws CommandSyntaxException;

    RegistryKey<World> dimensionKey(CommandContext<?> context, String name) throws CommandSyntaxException;

    ServerPlayerEntity player(CommandContext<?> context, String name) throws CommandSyntaxException;

    ServerPlayerEntity playerOrSelf(CommandContext<?> context, String name) throws CommandSyntaxException;

    Collection<ServerPlayerEntity> players(CommandContext<?> context, String name) throws CommandSyntaxException;

    void sendSuccess(Object source, ITextComponent message, boolean notifyAdmins);

    void sendFailure(Object source, ITextComponent message);

    Object literal(String name);

    void executes(Object commandNode, BaniraCommandExecutor executor);

    void then(Object parentNode, Object childNode);

    void register(Object dispatcher, Object commandNode);

    void onRegisterDispatcher(Consumer<Object> callback);

    void dispatchRegisterDispatcher(Object dispatcher);
}
