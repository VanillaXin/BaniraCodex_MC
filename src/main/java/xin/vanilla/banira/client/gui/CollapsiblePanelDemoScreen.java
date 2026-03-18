package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;

/**
 * 折叠面板控件演示界面
 */
public class CollapsiblePanelDemoScreen extends BaniraScreen {

    private static final int PADDING = 16;
    private static final int PANEL_WIDTH = 300;
    private static final int ROW_HEIGHT = 22;
    private static final int DROPDOWN_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 24;

    public CollapsiblePanelDemoScreen(@Nullable Screen parent) {
        super(Component.transClientAuto(BaniraCodex.MODID, "collapsible_panel_demo_title").toVanilla());
        if (parent != null) {
            previousScreen(parent);
        }
        BaniraScreen.inheritThemeAndSeason(this, parent, null, null);
    }

    public static void open(@Nullable Screen parent) {
        Minecraft.getInstance().setScreen(new CollapsiblePanelDemoScreen(parent));
    }

    @Override
    protected void initWidgets() {
        int w = width;
        int startX = (w - PANEL_WIDTH) / 2;
        int startY = PADDING;

        // region 根级折叠面板
        CollapsiblePanelWidget panel = CollapsiblePanelWidget.createAutoHeight(this, startX, startY, PANEL_WIDTH);
        panel.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        panel.id("panel");
        panel.text("折叠面板演示 - 多控件与深层嵌套");
        panel.expanded(true);

        panel.addChildAuto(new LabelWidget(this).text(Text.literal("顶部说明").color(EnumMCColor.DARK_RED.getColor())));
        InputWidget topInput = new InputWidget(this);
        topInput.maxLength(64).text("在此输入...");
        panel.addChildAuto(topInput, ROW_HEIGHT);
        panel.addChildAuto(new LabelWidget(this).text("下方为嵌套子面板："));

        // region 子面板 A - 含输入框与下拉（宽度自适应父级内容区）
        CollapsiblePanelWidget inner1 = panel.createChildPanel();
        inner1.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        inner1.id("inner_1");
        inner1.text("子面板 A - 输入与下拉");
        inner1.expanded(true);
        inner1.addChildAuto(new LabelWidget(this).text("嵌套层级 1"));
        InputWidget inner1Input = new InputWidget(this);
        inner1Input.maxLength(32).text("输入框");
        inner1.addChildAuto(inner1Input, ROW_HEIGHT);
        inner1.addChildAuto(new DropdownSelectWidget(this)
                .options(Arrays.asList("选项一", "选项二", "选项三", "选项四"))
                .selectedValues(Collections.singletonList("选项一")), DROPDOWN_HEIGHT);
        panel.addCollapsibleChild(inner1);
        // endregion

        // region 子面板 B - 含更深嵌套
        CollapsiblePanelWidget inner2 = panel.createChildPanel();
        inner2.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        inner2.id("inner_2");
        inner2.text("子面板 B - 深层嵌套");
        inner2.expanded(true);
        inner2.addChildAuto(new LabelWidget(this).text("子面板 B 内容"));

        // 子面板 B1 - 含滑块（宽度自适应 inner2 内容区）
        CollapsiblePanelWidget inner2_1 = inner2.createChildPanel();
        inner2_1.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        inner2_1.id("inner_2_1");
        inner2_1.text("子面板 B1 - 滑块");
        inner2_1.expanded(true);
        inner2_1.addChildAuto(new LabelWidget(this).text("音量调节"));
        inner2_1.addChildAuto(new SliderWidget(this).minValue(0).maxValue(100).value(50).showValue(true), ROW_HEIGHT);

        // 子面板 B1a - 第四层嵌套（宽度自适应 inner2_1 内容区）
        CollapsiblePanelWidget inner2_1_1 = inner2_1.createChildPanel();
        inner2_1_1.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        inner2_1_1.id("inner_2_1_1");
        inner2_1_1.text("子面板 B1a - 第四层");
        inner2_1_1.expanded(true);
        InputWidget deepInput = new InputWidget(this);
        deepInput.maxLength(24).text("深层输入");
        inner2_1_1.addChildAuto(deepInput, ROW_HEIGHT);
        inner2_1_1.addChildAuto(new ButtonWidget(this).text("深层按钮").onClick(b -> {
        }), BUTTON_HEIGHT);
        inner2_1.addCollapsibleChild(inner2_1_1);

        inner2.addCollapsibleChild(inner2_1);
        panel.addCollapsibleChild(inner2);
        // endregion

        // region 子面板 C - 含下拉与按钮
        CollapsiblePanelWidget inner3 = panel.createChildPanel();
        inner3.borderBottomWidth(0).borderRightWidth(0).paddingLeft(2).paddingRight(0);
        inner3.id("inner_3");
        inner3.text("子面板 C - 下拉与按钮");
        inner3.expanded(true);
        inner3.addChildAuto(new DropdownSelectWidget(this)
                .options(Arrays.asList("红", "绿", "蓝", "黄", "紫"))
                .selectedValues(Collections.singletonList("绿")), DROPDOWN_HEIGHT);
        inner3.addChildAuto(new ButtonWidget(this).text("确认").onClick(b -> {
        }), BUTTON_HEIGHT);
        panel.addCollapsibleChild(inner3);
        // endregion

        addWidget(panel);
        // endregion

        // // region 关闭按钮
        // ButtonWidget closeBtn = new ButtonWidget(this);
        // closeBtn.id("close");
        // closeBtn.bounds(new ScreenCoordinate(startX, startY, 80, BUTTON_HEIGHT));
        // closeBtn.text(Component.transClientAuto(BaniraCodex.MODID, "close").toString());
        // closeBtn.onClick(b -> onClose());
        // addWidget(closeBtn);
        // // endregion
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
