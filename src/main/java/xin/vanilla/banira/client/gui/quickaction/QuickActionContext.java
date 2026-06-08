package xin.vanilla.banira.client.gui.quickaction;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/**
 * 点击或从下拉选择某项时回传给注册回调的上下文
 */
@Accessors(chain = true, fluent = true)
public class QuickActionContext {

    @Getter
    @Setter
    @Nullable
    private Screen currentScreen;

    @Getter
    @Setter
    private String entryId;

    @Getter
    @Setter
    private double mouseX;

    @Getter
    @Setter
    private double mouseY;
}
