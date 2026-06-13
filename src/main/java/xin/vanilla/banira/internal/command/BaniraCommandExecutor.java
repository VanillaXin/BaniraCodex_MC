package xin.vanilla.banira.internal.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * 内部命令执行回调；避免把 Brigadier 注册细节挂到 public platform 上。
 */
@FunctionalInterface
public interface BaniraCommandExecutor {
    int run(Object context) throws CommandSyntaxException;
}
