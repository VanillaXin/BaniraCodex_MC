package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.component.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.KeyEventManager;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.network.data.AdvancementData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class AdvancementSelectScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private final KeyEventManager keyManager = new KeyEventManager();

    private final Args args;

    private static final Component TITLE = Component.literal("AdvancementSelectScreen");

    // 每页显示行数
    private final int maxLine = 5;

    /**
     * 输入框
     */
    private BaniraTextField inputField;
    /**
     * 输入框文本
     */
    private String inputFieldText = "";
    /**
     * 搜索结果
     */
    private final List<AdvancementData> advancementList = new ArrayList<>();
    /**
     * 操作按钮
     */
    private final Map<Integer, OperationButton> OP_BUTTONS = new HashMap<>();
    /**
     * 进度按钮
     */
    private final List<OperationButton> ADVANCEMENT_BUTTONS = new ArrayList<>();
    /**
     * 当前选择的进度
     */
    private ResourceLocation currentAdvancement;
    /**
     * 显示模式
     */
    private boolean displayMode = true;

    private int bgX;
    private int bgY;
    private final double margin = 3;
    private double effectBgX = this.bgX + margin;
    private double effectBgY = this.bgY + 20;

    /**
     * 滚动条组件
     */
    private final ScrollBar scrollBar = new ScrollBar();

    /**
     * 上一次的加载状态，用于检测加载完成
     */
    private boolean wasLoading = false;

    /**
     * 操作按钮类型
     */
    @Getter
    enum OperationButtonType {
        TYPE(1),
        ADVANCEMENT(2),
        PROBABILITY(6),
        ;

        final int code;

        OperationButtonType(int code) {
            this.code = code;
        }

        static OperationButtonType valueOf(int code) {
            return Arrays.stream(values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
        }
    }

    public AdvancementSelectScreen(Args args) {
        super(TITLE.toTextComponent());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.currentAdvancement = args.defaultAdvancement();
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        /**
         * 父级 Screen
         */
        private Screen parentScreen;
        /**
         * 默认值
         */
        private ResourceLocation defaultAdvancement = BaniraCodex.resourceFactory().empty();
        /**
         * 输入数据回调
         */
        private Consumer<ResourceLocation> onDataReceived1;
        /**
         * 输入数据回调
         */
        private Function<ResourceLocation, String> onDataReceived2;
        /**
         * 是否要显示该界面, 若为false则直接关闭当前界面并返回到调用者的 Screen
         */
        private Supplier<Boolean> shouldClose;

        public Args onDataReceived(Consumer<ResourceLocation> onDataReceived) {
            this.onDataReceived1 = onDataReceived;
            return this;
        }

        public Args onDataReceived(Function<ResourceLocation, String> onDataReceived) {
            this.onDataReceived2 = onDataReceived;
            return this;
        }

        public void validate() {
            Objects.requireNonNull(this.parentScreen());
            if (this.onDataReceived1() == null)
                Objects.requireNonNull(this.onDataReceived2());
            if (this.onDataReceived2() == null)
                Objects.requireNonNull(this.onDataReceived1());
        }

    }

    @Override
    protected void init() {
        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            Minecraft.getInstance().setScreen(args.parentScreen());

        // 确保本地有进度数据
        AdvancementUtils.ensureAdvancementData();

        // 初始化加载状态跟踪
        this.wasLoading = AdvancementUtils.isLoading();

        this.updateSearchResults();
        this.updateLayout();
        // 创建文本输入框
        this.inputField = AbstractGuiUtils.newTextFieldWidget(this.font, bgX, bgY, 112, 15, Component.empty())
                .hint("搜索进度...");
        this.inputField.setResponder(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        this.inputField.setValue(this.inputFieldText);
        this.addButton(this.inputField);

        int buttonY = (int) (this.bgY + (20 + (AbstractGuiUtils.ITEM_ICON_SIZE + 3) * 5 + margin));
        int buttonWidth = (int) (56 - this.margin * 2);

        // 创建提交按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + 56 + this.margin),
                buttonY,
                buttonWidth,
                20,
                Component.translatableClient(EnumI18nType.OPTION, "submit").toTextComponent(),
                button -> {
                    if (this.currentAdvancement == null) {
                        Minecraft.getInstance().setScreen(args.parentScreen());
                    } else {
                        // 获取选择的数据，并执行回调
                        ResourceLocation resourceLocation = this.currentAdvancement;
                        if (args.onDataReceived1() != null) {
                            args.onDataReceived1().accept(resourceLocation);
                            Minecraft.getInstance().setScreen(args.parentScreen());
                        } else if (args.onDataReceived2() != null) {
                            String result = args.onDataReceived2().apply(resourceLocation);
                            if (StringUtils.isNotNullOrEmpty(result)) {
                                // this.errorText = Text.literal(result).setColorArgb(0xFFFF0000);
                            } else {
                                Minecraft.getInstance().setScreen(args.parentScreen());
                            }
                        }
                    }
                }
        ));

        // 创建取消按钮
        this.addButton(new BaniraButton(
                (int) (this.bgX + this.margin),
                buttonY,
                buttonWidth,
                20,
                Component.translatableClient(EnumI18nType.OPTION, "cancel").toTextComponent(),
                button -> Minecraft.getInstance().setScreen(args.parentScreen())
        ));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        keyManager.refresh(mouseX, mouseY);
        // 绘制背景
        this.renderBackground(matrixStack);
        AbstractGuiUtils.fill(matrixStack, (int) (this.bgX - this.margin), (int) (this.bgY - this.margin), (int) (112 + this.margin * 2), (int) (20 + (AbstractGuiUtils.ITEM_ICON_SIZE + 3) * 5 + 20 + margin * 2 + 5), 0xCCC6C6C6, 2);
        AbstractGuiUtils.fillOutLine(matrixStack, (int) (this.effectBgX - this.margin), (int) (this.effectBgY - this.margin), 104, (int) ((AbstractGuiUtils.ITEM_ICON_SIZE + this.margin) * this.maxLine + this.margin), 1, 0xFF000000, 1);
        super.render(matrixStack, mouseX, mouseY, delta);
        // 保存输入框的文本, 防止窗口重绘时输入框内容丢失
        this.inputFieldText = this.inputField.getValue();

        // 检测加载状态变化
        boolean isLoading = AdvancementUtils.isLoading();
        if (this.wasLoading && !isLoading) {
            this.updateSearchResults();
        }
        this.wasLoading = isLoading;

        if (isLoading) {
            String loadingText = "加载中...";
            int textWidth = this.font.width(loadingText);
            int textX = this.width / 2 - textWidth / 2;
            int textY = (int) (this.effectBgY + (AbstractGuiUtils.ITEM_ICON_SIZE + this.margin) * this.maxLine / 2.0 - this.font.lineHeight / 2.0);
            this.font.drawShadow(matrixStack, loadingText, textX, textY, 0xFFFFFFFF);
        }

        this.renderButton(matrixStack);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        keyManager.mouseScrolled(delta, mouseX, mouseY);
        if (scrollBar.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        keyManager.mouseClicked(button, mouseX, mouseY);
        AtomicBoolean flag = new AtomicBoolean(false);
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_4) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            flag.set(true);
        } else if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT || button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            // 优先处理滚动条
            if (scrollBar.mouseClicked(mouseX, mouseY, button)) {
                flag.set(true);
            } else {
                OP_BUTTONS.forEach((key, value) -> {
                    if (value.isHovered()) {
                        value.setPressed(true);
                    }
                });
                // 进度按钮
                ADVANCEMENT_BUTTONS.forEach(bt -> bt.setPressed(bt.isHovered()));
            }
        }
        return flag.get() ? flag.get() : super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        keyManager.refresh(mouseX, mouseY);
        AtomicBoolean flag = new AtomicBoolean(false);
        AtomicBoolean updateSearchResults = new AtomicBoolean(false);
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT || button == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT) {
            // 优先处理滚动条
            if (scrollBar.mouseReleased(mouseX, mouseY, button)) {
                flag.set(true);
            } else {
                // 控制按钮
                OP_BUTTONS.forEach((key, value) -> {
                    if (value.isHovered() && value.isPressed()) {
                        this.handleOperation(value, button, flag, updateSearchResults);
                    }
                    value.setPressed(false);
                });
                // 进度按钮
                ADVANCEMENT_BUTTONS.forEach(bt -> {
                    if (bt.isHovered() && bt.isPressed()) {
                        this.handleAdvancement(bt, button, flag);
                    }
                    bt.setPressed(false);
                });
            }
            if (updateSearchResults.get()) {
                this.updateSearchResults();
            }
        }
        keyManager.mouseReleased(button, mouseX, mouseY);
        return flag.get() ? flag.get() : super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        keyManager.mouseMoved(mouseX, mouseY);
        // 更新滚动条
        scrollBar.mouseMoved(mouseX, mouseY);
        // 控制按钮
        OP_BUTTONS.forEach((key, value) -> value.setHovered(value.isMouseOverEx(mouseX, mouseY)));
        // 进度按钮
        ADVANCEMENT_BUTTONS.forEach(bt -> bt.setHovered(bt.isMouseOverEx(mouseX, mouseY)));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        keyManager.keyPressed(keyCode);
        if (keyManager.onlyEscapePressed() || (keyManager.onlyBackspacePressed() && !this.inputField.isFocused())) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            return true;
        } else if (keyManager.onlyEnterPressed() && this.inputField.isFocused()) {
            this.updateSearchResults();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        keyManager.keyReleased(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateLayout() {
        this.bgX = this.width / 2 - 56;
        this.bgY = this.height / 2 - 63;
        this.effectBgX = this.bgX + margin;
        this.effectBgY = this.bgY + 20;

        // 初始化操作按钮
        this.OP_BUTTONS.put(OperationButtonType.TYPE.getCode(), new OperationButton(OperationButtonType.TYPE.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            ItemStack itemStack = new ItemStack(this.displayMode ? Items.CHEST : Items.COMPASS);
            this.itemRenderer.renderGuiItem(itemStack, (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            int total = AdvancementUtils.getAllAdvancements().size();
            int displayableCount = AdvancementUtils.getDisplayableAdvancements().size();
            Text text = Text.translatable(BaniraCodex.MODID,
                    EnumI18nType.TIPS,
                    (this.displayMode ? "advancement_select_list_icon_mode" : "advancement_select_list_all_mode"),
                    (this.displayMode ? displayableCount : total));
            context.button.setTooltip(text);
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));
        this.OP_BUTTONS.put(OperationButtonType.ADVANCEMENT.getCode(), new OperationButton(OperationButtonType.ADVANCEMENT.getCode(), context -> {
            // 绘制背景
            int lineColor = context.button.isHovered() ? 0xEEFFFFFF : 0xEE000000;
            AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 0xEE707070, 2);
            AbstractGuiUtils.fillOutLine(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), 1, lineColor, 2);
            ItemStack iconStack = new ItemStack(Items.BARRIER);
            String title = "-";
            if (this.currentAdvancement != null) {
                Optional<AdvancementData> dataOpt = AdvancementUtils.findAdvancementByRegistry(this.currentAdvancement.toString());
                if (dataOpt.isPresent()) {
                    AdvancementData data = dataOpt.get();
                    iconStack = data.getDisplayInfo().getIcon();
                    title = data.getDisplayInfo().getTitle().getString();
                }
            }
            this.itemRenderer.renderGuiItem(iconStack, (int) context.button.getX() + 2, (int) context.button.getY() + 2);
            context.button.setTooltip(Text.literal(title));
        }).setX(this.bgX - AbstractGuiUtils.ITEM_ICON_SIZE - 2 - margin - 3).setY(this.bgY + margin + AbstractGuiUtils.ITEM_ICON_SIZE + 4 + 1).setWidth(AbstractGuiUtils.ITEM_ICON_SIZE + 4).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 4));

        // 初始化滚动条位置
        double bgWidth = 104;
        double bgHeight = (AbstractGuiUtils.ITEM_ICON_SIZE + margin) * maxLine - margin;
        scrollBar.x(effectBgX + bgWidth + 2)
                .y(effectBgY - margin + 1)
                .width(5)
                .height((int) (bgHeight + margin + 1));

        // 进度列表
        this.ADVANCEMENT_BUTTONS.clear();
        for (int i = 0; i < maxLine; i++) {
            ADVANCEMENT_BUTTONS.add(new OperationButton(i, context -> {
                int i1 = context.button.getOperation();
                int scrollRow = advancementList.size() > maxLine ? scrollBar.scrollOffset() : 0;
                int index = scrollRow + i1;
                if (index >= 0 && index < advancementList.size()) {
                    AdvancementData advancementData = advancementList.get(index);
                    double effectX = effectBgX;
                    double effectY = effectBgY + i1 * (AbstractGuiUtils.ITEM_ICON_SIZE + margin);
                    // 绘制背景
                    int bgColor;
                    if (context.button.isHovered() || (advancementData.getId().equals(this.currentAdvancement))) {
                        bgColor = 0xEE7CAB7C;
                    } else {
                        bgColor = 0xEE707070;
                    }
                    context.button.setX(effectX - 1).setY(effectY - 1).setWidth(100).setHeight(AbstractGuiUtils.ITEM_ICON_SIZE + 2)
                            .setId(advancementData.getId().toString());

                    AbstractGuiUtils.fill(context.matrixStack, (int) context.button.getX(), (int) context.button.getY(), (int) context.button.getWidth(), (int) context.button.getHeight(), bgColor);
                    FontDrawArgs drawArgs = FontDrawArgs.of(Text.literal(advancementData.getDisplayInfo().getTitle().getString()).stack(context.matrixStack).font(this.font))
                            .x(context.button.getX() + AbstractGuiUtils.ITEM_ICON_SIZE + this.margin * 2)
                            .y(context.button.getY() + (AbstractGuiUtils.ITEM_ICON_SIZE + 4 - this.font.lineHeight) / 2.0)
                            .maxWidth((int) context.button.getWidth() - AbstractGuiUtils.ITEM_ICON_SIZE - 4)
                            .wrap(false);
                    AbstractGuiUtils.drawLimitedText(drawArgs);
                    this.itemRenderer.renderGuiItem(advancementData.getDisplayInfo().getIcon(), (int) (context.button.getX() + this.margin), (int) context.button.getY());
                    context.button.setTooltip(Text.literal(advancementData.getDisplayInfo().getTitle().getString() + "\n" + advancementData.getDisplayInfo().getDescription().getString()));
                } else {
                    context.button.setX(0).setY(0).setWidth(0).setHeight(0).setId("");
                }
            }));
        }
    }

    /**
     * 更新搜索结果
     */
    private void updateSearchResults() {
        String s = this.inputField == null ? null : this.inputField.getValue();
        this.advancementList.clear();

        AdvancementUtils.ensureAdvancementData();

        // 根据显示模式和搜索关键字获取进度列表
        if (StringUtils.isNotNullOrEmpty(s)) {
            if (this.displayMode) {
                this.advancementList.addAll(AdvancementUtils.searchDisplayableAdvancements(s));
            } else {
                this.advancementList.addAll(AdvancementUtils.searchAdvancements(s));
            }
        } else {
            if (this.displayMode) {
                this.advancementList.addAll(AdvancementUtils.getDisplayableAdvancements());
            } else {
                this.advancementList.addAll(AdvancementUtils.getAllAdvancements());
            }
        }
        scrollBar.setScrollOffset(0);
    }

    /**
     * 绘制按钮
     */
    private void renderButton(MatrixStack matrixStack) {
        for (OperationButton button : OP_BUTTONS.values()) button.render(matrixStack, keyManager);
        for (OperationButton button : ADVANCEMENT_BUTTONS) button.render(matrixStack, keyManager);

        // 更新并渲染滚动条
        int totalRows = advancementList.size();
        scrollBar.updateScrollParams(totalRows, maxLine);
        scrollBar.render(matrixStack);
    }

    private void handleAdvancement(OperationButton bt, int button, AtomicBoolean flag) {
        if (button == GLFWKey.GLFW_MOUSE_BUTTON_LEFT) {
            if (StringUtils.isNotNullOrEmpty(bt.getId())) {
                try {
                    ResourceLocation resourceLocation = new ResourceLocation(bt.getId());
                    this.currentAdvancement = resourceLocation;
                    LOGGER.debug("Select advancement: {}", resourceLocation);
                    flag.set(true);
                } catch (Exception e) {
                    LOGGER.warn("Invalid advancement id clicked: {}", bt.getId(), e);
                }
            }
        }
    }

    private void handleOperation(OperationButton bt, int button, AtomicBoolean flag, AtomicBoolean updateSearchResults) {
        if (bt.getOperation() == OperationButtonType.TYPE.getCode()) {
            this.displayMode = !this.displayMode;
            updateSearchResults.set(true);
            flag.set(true);
        }
        // 编辑进度ID
        else if (bt.getOperation() == OperationButtonType.ADVANCEMENT.getCode()) {
            String effectString = this.currentAdvancement != null ? this.currentAdvancement.toString() : "";
            StringInputScreen.Args args = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_advancement_json").shadow(true))
                            .message(Text.translatable(BaniraCodex.MODID, EnumI18nType.TIPS, "enter_something"))
                            .defaultValue(effectString)
                            .validator((input) -> {
                                try {
                                    new ResourceLocation(input.value());
                                } catch (Exception e) {
                                    return Component.translatableClient(EnumI18nType.TIPS, "advancement_json_s_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        String id = input.firstValue();
                        try {
                            this.currentAdvancement = new ResourceLocation(id);
                        } catch (Exception e) {
                            LOGGER.warn("Invalid advancement id input: {}", id, e);
                        }
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(args));
        }
    }

}
