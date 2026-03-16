package xin.vanilla.banira.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import xin.vanilla.banira.command.impl.HelpCommand;
import xin.vanilla.banira.command.impl.LanguageCommand;
import xin.vanilla.banira.command.impl.VirtualOpCommand;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.util.*;
import java.util.stream.Collectors;


public class BaniraCommand {

    /**
     * 帮助信息列表
     */
    public static List<KeyValue<String, EnumCommandType>> HELP_MESSAGE = new ArrayList<>();

    /**
     * LANGUAGE 子指令结构
     */
    public static final LiteralArgumentBuilder<CommandSource> LANGUAGE = LanguageCommand.create();

    /**
     * VIRTUAL_OP 子指令结构
     */
    public static final LiteralArgumentBuilder<CommandSource> VIRTUAL_OP = VirtualOpCommand.create();

    private static void refreshHelpMessage() {
        HELP_MESSAGE = Arrays.stream(EnumCommandType.values())
                .map(type -> {
                    String command = getCommand(type);
                    if (command != null && !command.isEmpty()) {
                        return new KeyValue<>(command, type);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(kv -> !kv.value().ignore())
                .sorted(Comparator.comparingInt(kv -> kv.value().sort()))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定指令类型的完整命令字符串
     */
    public static String getCommand(EnumCommandType type) {
        String prefix = CustomConfig.getCommandPrefix();
        if (prefix == null || prefix.isEmpty()) {
            prefix = "banira";
        }
        switch (type) {
            case HELP:
                return prefix + " help";
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return prefix + " " + CustomConfig.getCommandLanguage();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return prefix + " " + CustomConfig.getCommandVirtualOp();
            default:
                return prefix;
        }
    }

    /**
     * 获取指令前缀
     */
    public static String getCommandPrefix() {
        String prefix = CustomConfig.getCommandPrefix();
        return (prefix == null || prefix.isEmpty()) ? "banira" : prefix.trim();
    }

    /**
     * 注册命令到命令调度器
     *
     * @param dispatcher 命令调度器
     */
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        refreshHelpMessage();

        LiteralArgumentBuilder<CommandSource> mainCommand = Commands.literal(getCommandPrefix());

        mainCommand.executes(HelpCommand::execute);

        for (EnumCommandType type : EnumCommandType.values()) {
            if (type.instance() != null) {
                mainCommand.then(type.instance().get());
            }
        }
        dispatcher.register(mainCommand);
    }
}
