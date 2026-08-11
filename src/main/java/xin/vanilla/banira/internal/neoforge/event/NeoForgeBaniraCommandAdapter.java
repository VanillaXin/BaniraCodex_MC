package xin.vanilla.banira.internal.neoforge.event;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import xin.vanilla.banira.command.BaniraCommand;

/**
 * NeoForge 命令注册事件适配层，公共入口只暴露 Brigadier dispatcher。
 */
public final class NeoForgeBaniraCommandAdapter {

    private NeoForgeBaniraCommandAdapter() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        BaniraCommand.register(event.getDispatcher());
    }
}
