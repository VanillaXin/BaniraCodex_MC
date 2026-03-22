package xin.vanilla.banira.client.gui;

import xin.vanilla.banira.BaniraComponent;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.TagListEditorWidget;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * 可折叠标签列表编辑控件演示界面
 */
public class TagListEditorDemoScreen extends BaniraScreen {

    private static final int PADDING = 16;
    private static final int PANEL_WIDTH = 300;

    public TagListEditorDemoScreen(@Nullable Screen parent) {
        super(BaniraComponent.get().transClientAuto("tag_list_editor_demo_title").toVanilla());
        if (parent != null) {
            previousScreen(parent);
        }
        BaniraScreen.inheritThemeAndSeason(this, parent, null, null);
    }

    public static void open(@Nullable Screen parent) {
        Minecraft.getInstance().setScreen(new TagListEditorDemoScreen(parent));
    }

    @Override
    protected void initWidgets() {
        int w = width;
        int startX = (w - PANEL_WIDTH) / 2;
        int startY = PADDING;

        // region 可折叠标签列表编辑控件演示
        TagListEditorWidget tagListText = new TagListEditorWidget(this);
        tagListText.bounds(new ScreenCoordinate(startX, startY, PANEL_WIDTH, TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagListText.text("标签列表（文本）");
        tagListText.itemType(TagListEditorWidget.ItemType.TEXT);
        tagListText.items(Arrays.asList("标签A", "标签B", "标签C"));
        tagListText.onListChanged(list -> {
        });
        addWidget(tagListText);
        startY += TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT + 8;

        TagListEditorWidget tagListEnum = new TagListEditorWidget(this);
        tagListEnum.bounds(new ScreenCoordinate(startX, startY, PANEL_WIDTH, TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagListEnum.text("标签列表（枚举）");
        tagListEnum.itemType(TagListEditorWidget.ItemType.ENUM);
        tagListEnum.enumOptions("选项A", "选项B", "选项C");
        addWidget(tagListEnum);
        startY += TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT + 8;

        TagListEditorWidget tagListBool = new TagListEditorWidget(this);
        tagListBool.bounds(new ScreenCoordinate(startX, startY, PANEL_WIDTH, TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagListBool.text("标签列表（布尔）");
        tagListBool.itemType(TagListEditorWidget.ItemType.BOOLEAN);
        addWidget(tagListBool);
        startY += TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT + 8;

        TagListEditorWidget tagListNum = new TagListEditorWidget(this);
        tagListNum.bounds(new ScreenCoordinate(startX, startY, PANEL_WIDTH, TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagListNum.text("标签列表（数值）");
        tagListNum.itemType(TagListEditorWidget.ItemType.NUMBER);
        tagListNum.items(Arrays.asList(1.0, 2.5, 100.0));
        addWidget(tagListNum);
        // endregion

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
