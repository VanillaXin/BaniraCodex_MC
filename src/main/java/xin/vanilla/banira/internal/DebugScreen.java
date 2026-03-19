package xin.vanilla.banira.internal;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.GLFWKeyUtils;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.TestConfig;
import xin.vanilla.banira.internal.event.ModEventHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;


@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugScreen extends BaniraScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private String content = "";
    private int contentLines = 2;
    private int contentLength = 20;
    private int fontSize = 9;
    private boolean warp = false;


    protected DebugScreen() {
        super(Component.empty().toVanilla());
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        DropdownSelectWidget dropdownSingle = new DropdownSelectWidget(this);
        dropdownSingle.id("dropdown_single");
        dropdownSingle.bounds(new ScreenCoordinate(110, 20, 150, 24));
        dropdownSingle.options(Arrays.asList("自动", "春", "夏", "秋", "冬"));
        dropdownSingle.text("选择季节");
        dropdownSingle.selectedValues(Collections.singletonList("自动"));
        dropdownSingle.onSelectionChanged(values -> {
            LOGGER.debug("单选下拉: {}", values);
            season(EnumSeason.valueOfEx(values.isEmpty() ? "自动" : values.get(0)));
        });
        addWidget(dropdownSingle);

        DropdownSelectWidget dropdownMulti = new DropdownSelectWidget(this);
        dropdownMulti.id("dropdown_multi_fruit");
        dropdownMulti.bounds(new ScreenCoordinate(110, 50, 150, 24));
        dropdownMulti.options(Arrays.asList("苹果", "香蕉", "橙子", "葡萄", "草莓", "西瓜"));
        dropdownMulti.text("选择水果（可多选）");
        dropdownMulti.multiSelect(true);
        dropdownMulti.onSelectionChanged(values -> LOGGER.debug("多选下拉水果: {}", values));
        addWidget(dropdownMulti);

        DropdownSelectWidget dropdownMultiEx = new DropdownSelectWidget(this);
        dropdownMultiEx.id("dropdown_multi_vegetable");
        dropdownMultiEx.bounds(new ScreenCoordinate(110, 80, 150, 24));
        dropdownMultiEx.options(Arrays.asList("白菜", "菠菜", "油菜", "生菜", "空心菜", "韭菜", "芹菜", "香菜", "茼蒿", "苋菜",
                "芥蓝", "小白菜", "大白菜", "卷心菜", "紫甘蓝", "羽衣甘蓝", "西兰花", "菜花", "芥菜", "雪里红",
                "萝卜", "胡萝卜", "白萝卜", "青萝卜", "樱桃萝卜", "土豆", "红薯", "紫薯", "山药", "芋头",
                "洋葱", "大蒜", "蒜苗", "蒜黄", "大葱", "小葱", "韭菜苔", "芦笋", "竹笋", "莴笋",
                "莲藕", "菱角", "荸荠", "慈姑", "百合", "生姜", "洋姜", "豆芽", "绿豆芽", "黄豆芽",
                "豇豆", "四季豆", "扁豆", "荷兰豆", "毛豆", "蚕豆", "豌豆", "黄豆", "绿豆", "红豆",
                "黄瓜", "冬瓜", "南瓜", "苦瓜", "丝瓜", "西葫芦", "佛手瓜", "木瓜", "西红柿", "茄子",
                "辣椒", "青椒", "彩椒", "甜椒", "秋葵", "玉米", "鲜玉米", "茴香", "萝卜苗", "豌豆苗",
                "黑木耳", "银耳", "香菇", "金针菇", "平菇", "杏鲍菇", "口蘑", "猴头菇", "茶树菇", "竹荪",
                "海带", "紫菜", "裙带菜", "石花菜", "地皮菜", "蕨菜", "马齿苋", "荠菜", "蒲公英", "鱼腥草"));
        dropdownMultiEx.text("选择蔬菜（可多选）");
        dropdownMultiEx.multiSelect(true);
        dropdownMultiEx.onSelectionChanged(values -> LOGGER.debug("多选下拉蔬菜: {}", values));
        addWidget(dropdownMultiEx);

        String cPlus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_C, GLFWKey.GLFW_KEY_EQUAL);
        String cMinus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_C, GLFWKey.GLFW_KEY_MINUS);
        String ePlus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_E, GLFWKey.GLFW_KEY_EQUAL);
        String eMinus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_E, GLFWKey.GLFW_KEY_MINUS);
        String fPlus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_F, GLFWKey.GLFW_KEY_EQUAL);
        String fMinus = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_F, GLFWKey.GLFW_KEY_MINUS);
        String ctrlW = GLFWKeyUtils.getKeyDisplayString(GLFWKey.GLFW_KEY_LEFT_CONTROL, GLFWKey.GLFW_KEY_W);

        addTooltipLabel(20, 20, cPlus + " 增加，" + cMinus + " 减少");
        addTooltipLabel(20, 40, ePlus + " 增加，" + eMinus + " 减少");
        addTooltipLabel(20, 60, fPlus + " 增加，" + fMinus + " 减少");
        addTooltipLabel(20, 80, ctrlW + " 切换");
        addTooltipLabel(20, 100, "N+方向键 指定位置（支持组合：↑↓←→）");
        addTooltipLabel(20, 120, "Page Up 成就选择，Page Down 效果选择");

        ButtonWidget testConfigBtn = new ButtonWidget(this);
        testConfigBtn.id("test_config_editor");
        testConfigBtn.bounds(new ScreenCoordinate(110, 140, 75, 24));
        testConfigBtn.text("配置编辑测试");
        testConfigBtn.onClick(b -> ConfigEditorScreen.open(TestConfig.get().holder(), this));
        addWidget(testConfigBtn);

        ButtonWidget configBtn = new ButtonWidget(this);
        configBtn.id("config_editor");
        configBtn.bounds(new ScreenCoordinate(190, 140, 75, 24));
        configBtn.text("配置编辑");
        configBtn.onClick(b -> ConfigEditorScreen.open(CommonConfig.get().holder(), this));
        addWidget(configBtn);

        ButtonWidget collapsibleDemoBtn = new ButtonWidget(this);
        collapsibleDemoBtn.id("collapsible_panel_demo");
        collapsibleDemoBtn.bounds(new ScreenCoordinate(270, 140, 75, 24));
        collapsibleDemoBtn.text("折叠面板测试");
        collapsibleDemoBtn.onClick(b -> CollapsiblePanelDemoScreen.open(this));
        addWidget(collapsibleDemoBtn);

        ButtonWidget tagListDemoBtn = new ButtonWidget(this);
        tagListDemoBtn.id("tag_list_editor_demo");
        tagListDemoBtn.bounds(new ScreenCoordinate(350, 140, 75, 24));
        tagListDemoBtn.text("标签列表测试");
        tagListDemoBtn.onClick(b -> TagListEditorDemoScreen.open(this));
        addWidget(tagListDemoBtn);

        addPresetStyleButtons();
    }

    private void addPresetStyleButtons() {
        int btnSize = 28;
        int gap = 4;
        int startX = 280;
        int startY = 44;
        ButtonWidget.PresetStyle[] styles = {
                ButtonWidget.PresetStyle.CLOSE,
                ButtonWidget.PresetStyle.MINUS,
                ButtonWidget.PresetStyle.PLUS,
                ButtonWidget.PresetStyle.MAXIMIZE,
                ButtonWidget.PresetStyle.ARROW_UP,
                ButtonWidget.PresetStyle.ARROW_DOWN,
                ButtonWidget.PresetStyle.ARROW_LEFT,
                ButtonWidget.PresetStyle.ARROW_RIGHT,
        };
        for (int i = 0; i < styles.length; i++) {
            int col = i % 4;
            int row = i / 4;
            int x = startX + col * (btnSize + gap);
            int y = startY + row * (btnSize + gap);
            ButtonWidget btn = new ButtonWidget(this);
            btn.id("preset_" + styles[i].name());
            btn.bounds(new ScreenCoordinate(x, y, btnSize, btnSize));
            if (styles[i] == ButtonWidget.PresetStyle.CLOSE) {
                btn.presetStyleClose().padding(4);
            } else {
                btn.presetStyle(styles[i]).padding(4).borderWidth(1);
            }
            btn.onClick(b -> LOGGER.debug("Preset button clicked: {}", b.id()));
            addWidget(btn);
        }
    }

    private void addTooltipLabel(int x, int y, String tooltipText) {
        TooltipWidget w = new TooltipWidget(this, new ScreenCoordinate(x, y, 40, 18));
        w.text(Component.literal(tooltipText)).vanillaTooltip(true);
        w.visible(true);
        addWidget(w);
    }

    /**
     * 根据方向键组合解析位置：↑↓ 控制上下，←→ 控制左右，支持组合；↑↓ 或 ←→ 同时按为 CENTER
     */
    private EnumPosition positionFromArrowKeys() {
        boolean up = inputState.isKeyPressed(GLFWKey.GLFW_KEY_UP);
        boolean down = inputState.isKeyPressed(GLFWKey.GLFW_KEY_DOWN);
        boolean left = inputState.isKeyPressed(GLFWKey.GLFW_KEY_LEFT);
        boolean right = inputState.isKeyPressed(GLFWKey.GLFW_KEY_RIGHT);
        if ((up && down) || (left && right)) return EnumPosition.CENTER;
        if (up && left) return EnumPosition.TOP_LEFT;
        if (up && right) return EnumPosition.TOP_RIGHT;
        if (up) return EnumPosition.TOP_CENTER;
        if (down && left) return EnumPosition.BOTTOM_LEFT;
        if (down && right) return EnumPosition.BOTTOM_RIGHT;
        if (down) return EnumPosition.BOTTOM_CENTER;
        if (left) return EnumPosition.LEFT_CENTER;
        if (right) return EnumPosition.RIGHT_CENTER;
        return null;
    }

    /**
     * NotificationManager 测试：N+方向键指定位置（animation 默认 AUTO 使用位置对应动画）
     */
    private void addNotificationTest(EnumPosition position) {
        if (position == null) return;
        Notification n = Notification.ofComponent(
                Component.literal("NotificationManager 测试 - " + position.name()));
        n.position(position)
                .animation(EnumMoveType.SCALE_AND_FADE)
                .durationTime(3000);
        NotificationManager.get().addNotification(n);
    }

    @Override
    public void onRender(MatrixStack stack, float partialTicks) {

        ShapeDrawArgs bgRect = ShapeDrawArgs.rect(stack, 10, 10, this.width / 3f, this.height - 20, 0x88E8F4FF);
        bgRect.rect().radius(8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(bgRect);

        // AbstractGuiUtils.drawLine(stack, 20, 20, 150, 150, 1, 0x33FFFFFF);

        // // 白色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.BACKGROUND, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 50) / 2, (super.height - 50) / 2, 50, 50, 0xFFFFFFFF)
        // );
        // // 红色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.OVERLAY, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 10) / 2, (super.height - 10) / 2, 10, 10, 0xFFFF0000)
        // );
        // // 黑色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.FOREGROUND, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 30) / 2, (super.height - 30) / 2, 30, 30, 0xFF000000)
        // );

        ShapeDrawArgs circle = ShapeDrawArgs.circle(stack, super.width / 2f, super.height / 2f, 15, 0x4487CEEB);
        BaseShapeWidget.drawShape(circle);

        // ShapeDrawArgs ellipse = ShapeDrawArgs.ellipse(stack, super.width / 2f, super.height / 2f, 7.5f, 15, 0x33FFFFFF);
        // ellipse.ellipse().rotation((System.currentTimeMillis() / 50d) % 360);
        // BaseShapeWidget.drawShape(ellipse);

        ShapeDrawArgs ellipseRing = ShapeDrawArgs.ellipse(stack, super.width / 2f, super.height / 2f, 7.5f, 15, 0x4487CEEB);
        ellipseRing.ellipse().rotation((System.currentTimeMillis() / 50d) % 360).border(2);
        BaseShapeWidget.drawShape(ellipseRing);

        // ShapeDrawArgs ring = ShapeDrawArgs.circle(stack, super.width / 2f, super.height / 2f, 17, 0x33FFFFFF);
        // ring.circle().border(0.5f);
        // BaseShapeWidget.drawShape(ring);

        // ShapeDrawArgs sector = ShapeDrawArgs.sector(stack, (super.width - 35) / 2f, (super.height - 35) / 2f, 35, 0, 75, 0x33FFFFFF);
        // BaseShapeWidget.drawShape(sector);

        // ShapeDrawArgs sectoredRing = ShapeDrawArgs.sectorRing(stack, super.width / 2f, super.height / 2f, 35, 30, 180, 255, 0x33FFFFFF);
        // BaseShapeWidget.drawShape(sectoredRing);

        ShapeDrawArgs rect1 = ShapeDrawArgs.rect(stack, (super.width - 60) / 2f, (super.height - 60) / 2f, 60, 60, 0x44B8D4F0);
        rect1.rect().topRight(15).bottomLeft(15).bottomRight(15);
        BaseShapeWidget.drawShape(rect1);

        ShapeDrawArgs rect11 = ShapeDrawArgs.rect(stack, (super.width - 70) / 2f, (super.height - 70) / 2f, 70, 70, 0x44B8D4F0);
        rect11.rect().border(4).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE).topLeft(4).topRight(35).bottomLeft(35).bottomRight(16);
        BaseShapeWidget.drawShape(rect11);

        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("ButtonWidget 预置样式")).x(280).y(20).padding(4).margin(0).inScreen(false));

        renderWidgets(stack, partialTicks);

        int hudY = 1;
        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("内容行数：" + contentLines)).x(20).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("内容长度：" + contentLength)).x(20).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("字体大小：" + fontSize)).x(20).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("自动换行：" + warp)).x(20).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        LabelWidget.drawLimitedText(FontDrawArgs.ofPopo(Text.literal("N 通知测试")).x(20).y(20 * hudY++).padding(4).margin(0).inScreen(false));

        if (StringUtils.isNullOrEmptyEx(content)) genContent();
        if (inputState.isPressingLeftEx()) {
            // 颜色绘制
            TooltipWidget.drawPopupMessage(stack, FontDrawArgs.ofPopo(Text.literal(content)
                                    .stack(stack)
                                    .font(super.font)
                                    .align(EnumAlignment.CENTER))
                            .x(inputState.mouseX()).y(inputState.mouseY()).fontSize(fontSize).align(EnumAlignment.CENTER)
                            .wrap(warp).maxWidth(warp ? AbstractGuiUtils.getStringWidth(this.content) / 2 : 0)
                            .popupUseTexture(false),
                    getEffectiveTheme(), season());
        } else if (inputState.isPressingRightEx()) {
            // 纹理绘制
            TooltipWidget.drawPopupMessageWithSeasonTexture(stack, FontDrawArgs.ofPopo(Text.literal(content)
                                    .stack(stack)
                                    .font(super.font)
                                    .align(EnumAlignment.CENTER))
                            .x(inputState.mouseX()).y(inputState.mouseY()).fontSize(fontSize).align(EnumAlignment.CENTER)
                            .wrap(warp).maxWidth(warp ? AbstractGuiUtils.getStringWidth(this.content) / 2 : 0),
                    season());
        }
    }

    @Override
    protected void onMouseReleased(MouseReleasedHandleArgs eventArgs) {
        if (eventArgs.button() == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            this.popupOption.clear()
                    .addOptionWithId("opt_a", "选项 A - 示例功能", "这是选项 A 的提示")
                    .addOptionWithId("opt_b", "选项 B - 复制信息", "这是选项 B 的提示")
                    .addOptionWithId("opt_notice", "选项 C - 发送通知测试", "方向键+N 直接触发")
                    .addOptionWithId("opt_input", "选项 D - 打开文本输入")
                    .addOptionWithId("opt_item", "选项 E - 打开物品选择")
                    .addOptionWithId("opt_advancement", "选项 F - 打开成就选择")
                    .addOptionWithId("opt_effect", "选项 G - 打开效果选择")
                    .addOptionWithId("opt_notification_log", "选项 H - 通知日志", "查看所有通知记录")
                    .onSelect(e -> handlePopupSelect(e.id(), e.text()))
                    .showAt(eventArgs.mouseX(), eventArgs.mouseY());
        }
    }

    private void handlePopupSelect(String id, String selected) {
        LOGGER.debug("PopupOption 选中: id={}, text={}", id, selected);
        switch (id) {
            case "opt_notice":
                addNotificationTest(EnumPosition.TOP_RIGHT);
                break;
            case "opt_input":
                StringInputScreen.Args screenArgs = new StringInputScreen.Args()
                        .setParentScreen(this)
                        .addWidget(new StringInputScreen.Widget().name("input").title(Text.literal("enter_something")))
                        .addWidget(new StringInputScreen.Widget()
                                .name("input")
                                .title(Text.literal("enter_something"))
                                .type(StringInputScreen.WidgetType.NUMERIC))
                        .setCallback(input -> LOGGER.debug("Entered: {}", input.value("input")));
                Minecraft.getInstance().setScreen(new StringInputScreen(screenArgs));
                break;
            case "opt_item":
                Consumer<ItemStack> onItemSelect = is -> LOGGER.debug("Select itemStack: {}", ItemUtils.serializeItemStack(is));
                Minecraft.getInstance().setScreen(new ItemSelectScreen(new ItemSelectScreen.Args().parentScreen(this).onDataReceived(onItemSelect)));
                break;
            case "opt_advancement":
                Consumer<ResourceLocation> onAdvSelect = rl -> LOGGER.debug("Selected advancement: {}", rl);
                AdvancementSelectScreen.Args args = new AdvancementSelectScreen.Args()
                        .parentScreen(this)
                        .defaultAdvancement(Identifier.id().empty())
                        .onDataReceived(onAdvSelect);
                Minecraft.getInstance().setScreen(new AdvancementSelectScreen(args));
                break;
            case "opt_effect":
                Consumer<EffectInstance> onEffectSelect = ei -> LOGGER.debug("Selected effect: {}", EffectUtils.serializeEffectInstance(ei));
                EffectSelectScreen.Args effectArgs = new EffectSelectScreen.Args()
                        .parentScreen(this)
                        .defaultEffect(new EffectInstance(Effects.LUCK, 600, 0))
                        .onDataReceived(onEffectSelect);
                Minecraft.getInstance().setScreen(new EffectSelectScreen(effectArgs));
                break;
            case "opt_notification_log":
                Minecraft.getInstance().setScreen(new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen(this)));
                break;
            default:
                break;
        }
    }

    @Override
    public void onKeyReleased(KeyReleasedHandleArgs eventArgs) {
        if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_E)) {
            if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.contentLength++;
                genContent();
            } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.contentLength--;
                genContent();
            }
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_C)) {
            if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.contentLines++;
                genContent();
            } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.contentLines--;
                genContent();
            }
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_F)) {
            if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.fontSize++;
            } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.fontSize--;
            }
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_CONTROL) && inputState.isKeyPressed(GLFWKey.GLFW_KEY_W)) {
            this.warp = !this.warp;
        } else if (eventArgs.keyCode() == GLFWKey.GLFW_KEY_N) {
            addNotificationTest(positionFromArrowKeys());
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_INSERT)) {
            Minecraft.getInstance().setScreen(new ItemSelectScreen(new ItemSelectScreen.Args().parentScreen(this).onDataReceived((itemStack) -> {
                LOGGER.debug("Select itemStack: {}", ItemUtils.serializeItemStack(itemStack));
            })));
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_HOME)) {
            StringInputScreen.Args screenArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .name("name")
                            .title(Text.literal("enter_name"))
                            .validator((input) -> {
                                if (StringUtils.isNullOrEmptyEx(input.value())) {
                                    return Component.transClientAuto(BaniraCodex.MODID, "enter_something_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("author")
                            .title(Text.literal("enter_author_name"))
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("version")
                            .title(Text.literal("enter_version"))
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("description")
                            .title(Text.literal("enter_description"))
                            .allowEmpty(true)
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("color")
                            .title(Text.literal("enter_color"))
                            .type(StringInputScreen.WidgetType.COLOR)
                            .allowEmpty(true)
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("file")
                            .title(Text.literal("enter_file_name"))
                            .type(StringInputScreen.WidgetType.FILE)
                            .allowEmpty(true)
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("1")
                            .title(Text.literal("enter_1"))
                            .allowEmpty(true)
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("2")
                            .title(Text.literal("enter_2"))
                            .allowEmpty(true)
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("3")
                            .title(Text.literal("enter_3"))
                            .allowEmpty(true)
                    )
                    .setCallback(input -> LOGGER.debug("Entered name: {}", input.value("name")));
            Minecraft.getInstance().setScreen(new StringInputScreen(screenArgs));
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_PAGE_UP)) {
            AdvancementSelectScreen.Args args = new AdvancementSelectScreen.Args();
            args.parentScreen(this).defaultAdvancement(Identifier.id().empty());
            args.onDataReceived((Consumer<ResourceLocation>) rl -> LOGGER.debug("Selected advancement: {}", rl));
            Minecraft.getInstance().setScreen(new AdvancementSelectScreen(args));
        } else if (inputState.isKeyPressed(GLFWKey.GLFW_KEY_PAGE_DOWN)) {
            EffectSelectScreen.Args effectArgs = new EffectSelectScreen.Args()
                    .parentScreen(this)
                    .defaultEffect(new net.minecraft.potion.EffectInstance(net.minecraft.potion.Effects.LUCK, 600, 0));
            effectArgs.onDataReceived((Consumer<net.minecraft.potion.EffectInstance>) ei -> LOGGER.debug("Selected effect: {}", EffectUtils.serializeEffectInstance(ei)));
            Minecraft.getInstance().setScreen(new EffectSelectScreen(effectArgs));
        }
    }

    private void genContent() {
        StringBuilder content = new StringBuilder(Component.transClientAuto(BaniraCodex.MODID, "banira_codex").modId(BaniraCodex.MODID).toString()).append("\n");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < contentLines - 1; i++) {
            if (i == 0) {
                Component component = Component.literal("Copyright (c) %s ").appendArg(DateUtils.getYearPart(new Date()))
                        .append(Component.transClientAuto(BaniraCodex.MODID, "vanilla_xin").modId(BaniraCodex.MODID));
                content.append(component.toString()).append("\n");
            } else {
                RandomStringUtils.CharSource source;
                switch (random.nextInt(5)) {
                    case 0:
                        source = RandomStringUtils.CharSource.DIGITS;
                        break;
                    case 1:
                        source = RandomStringUtils.CharSource.ALPHANUMERIC;
                        break;
                    case 2:
                        source = RandomStringUtils.CharSource.SPECIAL_CHARACTERS;
                        break;
                    case 3:
                        source = RandomStringUtils.CharSource.ASCII_PRINTABLE;
                        break;
                    default:
                        source = RandomStringUtils.CharSource.CHINESE;
                        break;
                }
                content.append(RandomStringUtils.generate(random.nextInt((contentLength + 1) / 2) + contentLength / 2, source)).append("\n");
            }
        }
        this.content = content.substring(0, content.length() - 1);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (Minecraft.getInstance().screen == null && ModEventHandler.DEBUG_KEY.isDown()) {
                Minecraft.getInstance().setScreen(new DebugScreen());
            }
        }
    }

}
