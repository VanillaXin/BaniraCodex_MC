package xin.vanilla.banira.internal.forge.event;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xin.vanilla.banira.command.BaniraCommand;

/**
 * Forge 命令注册事件适配层，公共入口只暴露 Brigadier dispatcher。
 */
public final class ForgeBaniraCommandAdapter {

    private ForgeBaniraCommandAdapter() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        BaniraCommand.register(event.getDispatcher());
    }
}
