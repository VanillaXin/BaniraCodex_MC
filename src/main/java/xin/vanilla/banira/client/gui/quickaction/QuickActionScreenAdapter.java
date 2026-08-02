package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.gui.screen.Screen;

/** 具体模组可通过此扩展点屏蔽自己的背包按钮，并注册等价的 Banira 快捷入口。 */
public interface QuickActionScreenAdapter {
    boolean supports(Screen screen);

    void adopt(Screen screen, QuickActionRegistry registry);
}
