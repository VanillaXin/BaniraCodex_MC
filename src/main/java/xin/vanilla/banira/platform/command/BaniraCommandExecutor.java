package xin.vanilla.banira.platform.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Loader-neutral wrapper for command execution callbacks.
 */
@FunctionalInterface
public interface BaniraCommandExecutor {
    int run(Object context) throws CommandSyntaxException;
}
