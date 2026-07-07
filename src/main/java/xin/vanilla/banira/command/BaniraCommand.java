package xin.vanilla.banira.command;

import xin.vanilla.banira.command.impl.HelpCommand;
import xin.vanilla.banira.command.impl.LanguageCommand;
import xin.vanilla.banira.command.impl.VirtualOpCommand;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.config.CommonConfig;

import java.util.*;
import java.util.stream.Collectors;


public class BaniraCommand {
    /**
     * 可挂载到子 mod 命令树下的语言子命令节点。
     */
    public static final Object LANGUAGE = LanguageCommand.create();

    /**
     * 可挂载到子 mod 命令树下的虚拟权限子命令节点。
     */
    public static final Object VIRTUAL_OP = VirtualOpCommand.create();

    /**
     * 帮助信息列表
     */
    public static List<KeyValue<String, EnumCommandType>> HELP_MESSAGE = new ArrayList<>();

    /**
     * 与非精简版完全相同的精简版条目不进入帮助列表，避免同一指令出现两行
     */
    private static boolean helpEntryDistinctFromBaseCommand(KeyValue<String, EnumCommandType> kv) {
        EnumCommandType type = kv.value();
        if (!type.name().endsWith("_CONCISE")) {
            return true;
        }
        String shown = kv.key();
        String base = getCommand(type.replaceConcise());
        return shown == null || !shown.equals(base);
    }

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
                .filter(BaniraCommand::helpEntryDistinctFromBaseCommand)
                .sorted(Comparator.comparingInt(kv -> kv.value().sort()))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定指令类型的完整命令字符串
     */
    public static String getCommand(EnumCommandType type) {
        String prefix = CommonConfig.get().command().commandPrefix();
        switch (type) {
            case HELP:
                return prefix + " " + CommonConfig.get().command().commandHelp();
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return prefix + " " + CommonConfig.get().command().commandLanguage();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return prefix + " " + CommonConfig.get().command().commandVirtualOp();
            case EDIT_SERVER_CONFIG:
                return null;
            default:
                return prefix;
        }
    }

    /**
     * 获取指令前缀
     */
    public static String getCommandPrefix() {
        return CommonConfig.get().command().commandPrefix();
    }

    /**
     * 注册命令到命令调度器
     *
     * @param dispatcher 命令调度器
     */
    public static void register(Object dispatcher) {
        refreshHelpMessage();

        Object mainCommand = BaniraCommandAccess.literal(getCommandPrefix());

        BaniraCommandAccess.executes(mainCommand, HelpCommand::executeRaw);
        for (EnumCommandType type : EnumCommandType.values()) {
            if (type.instance() != null) {
                Object child = type.instance().get();
                BaniraCommandAccess.then(mainCommand, child);
            }
        }
        BaniraCommandAccess.register(dispatcher, mainCommand);
    }
}
