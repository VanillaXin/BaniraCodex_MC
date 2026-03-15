package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.EffectUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class EffectSelectScreen extends BaniraScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INPUT_H = 16;
    private static final int OP_BTN_SIZE = AbstractGuiUtils.ITEM_ICON_SIZE + 2;
    private static final int OP_BTN_GAP = 2;
    private static final int ITEM_SPACING = 1;
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_LINES = 5;
    private static final int BTN_H = 20;
    private static final int PANEL_MARGIN = 6;
    private static final int SCROLL_W = 5;

    private final Args args;

    private static final Component TITLE = Component.literal("EffectSelectScreen");

    private String inputFieldText = "";
    private final List<Effect> effectList = new ArrayList<>();
    @Nullable
    private InputWidget searchInputWidget;
    private final List<ButtonWidget> effectButtonWidgets = new ArrayList<>();
    @Nullable
    private ScrollbarWidget scrollbarWidget;
    private EffectInstance currentEffect;
    private boolean playerMode = false;

    private BaseWidget selectedEffectWidget;
    private ItemWidget typeButtonItemWidget;
    private EffectIconWidget effectButtonIconWidget;
    private TooltipWidget typeTooltip;
    private TooltipWidget effectTooltip;
    private TooltipWidget durationTooltip;
    private TooltipWidget amplifierTooltip;

    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;

    @Getter
    @Accessors(fluent = true)
    enum ButtonType {
        TYPE(1),
        EFFECT(2),
        DURATION(3),
        AMPLIFIER(4),
        ;

        final int code;

        ButtonType(int code) {
            this.code = code;
        }

        static ButtonType valueOf(int code) {
            return Arrays.stream(values()).filter(v -> v.code() == code).findFirst().orElse(null);
        }
    }

    public EffectSelectScreen(Args args) {
        super(TITLE.toVanilla());
        Objects.requireNonNull(args);
        args.validate();
        this.args = args;
        this.currentEffect = args.defaultEffect() != null ? EffectUtils.copyEffectInstance(args.defaultEffect()) : new EffectInstance(Effects.LUCK, 600, 0);
        BaniraScreen.inheritThemeAndSeason(this, args.parentScreen(), args.theme(), args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        private Screen parentScreen;
        private EffectInstance defaultEffect = new EffectInstance(Effects.LUCK, 600, 0);
        private Consumer<EffectInstance> onDataReceived1;
        private Function<EffectInstance, String> onDataReceived2;
        private Supplier<Boolean> shouldClose;
        @Nullable
        private EnumSeason season;
        @Nullable
        private BaniraColorConfig theme;

        public Args onDataReceived(Consumer<EffectInstance> onDataReceived) {
            this.onDataReceived1 = onDataReceived;
            return this;
        }

        public Args onDataReceived(Function<EffectInstance, String> onDataReceived) {
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
    protected void onInit() {
        if (args.shouldClose() != null && Boolean.TRUE.equals(args.shouldClose().get()))
            Minecraft.getInstance().setScreen(args.parentScreen());

        this.updateSearchResults();
    }

    @Override
    protected void initWidgets() {
        boolean useSeasonTooltip = true;

        int w = width;
        int h = height;
        int listH = MAX_LINES * ROW_HEIGHT;
        int listW = 200;
        int scrollGap = 2;

        int panelW = PANEL_MARGIN + listW + scrollGap + SCROLL_W + PANEL_MARGIN;
        int panelH = PANEL_MARGIN + INPUT_H + 4 + listH + 4 + BTN_H + PANEL_MARGIN;

        int totalW = OP_BTN_SIZE + OP_BTN_GAP + panelW;
        this.panelLeft = (w - totalW) / 2 + OP_BTN_SIZE + OP_BTN_GAP;
        this.panelTop = (h - panelH) / 2;
        this.panelW = panelW;
        this.panelH = panelH;

        int inputY = this.panelTop + PANEL_MARGIN;
        int inputX = this.panelLeft + PANEL_MARGIN;
        int inputW = this.panelW - PANEL_MARGIN * 2 - SCROLL_W - scrollGap;
        int listY = inputY + INPUT_H + 4;
        int listX = this.panelLeft + PANEL_MARGIN;
        int listItemW = listW - 2;
        int scrollX = listX + listW + scrollGap;
        int btnY = listY + listH + 4;
        int btnAreaW = this.panelW - PANEL_MARGIN * 2 - SCROLL_W - scrollGap;
        int btnW = (btnAreaW - PANEL_MARGIN) / 2;
        int cancelX = this.panelLeft + PANEL_MARGIN;
        int submitX = this.panelLeft + PANEL_MARGIN + btnW + PANEL_MARGIN;

        // 输入框
        searchInputWidget = new InputWidget(this);
        searchInputWidget.id("search_input");
        searchInputWidget.renderCoordinate(new ScreenCoordinate(inputX, inputY, inputW, INPUT_H));
        searchInputWidget.value(this.inputFieldText);
        searchInputWidget.text(Text.transAuto(BaniraCodex.MODID, "search_effect"));
        searchInputWidget.onTextChanged(text -> {
            if (!text.equals(this.inputFieldText)) {
                this.inputFieldText = text;
                this.updateSearchResults();
            }
        });
        addWidget(searchInputWidget);

        // 滚动条
        scrollbarWidget = new ScrollbarWidget(this);
        scrollbarWidget.id("scroll");
        scrollbarWidget.renderCoordinate(new ScreenCoordinate(scrollX, listY, SCROLL_W, listH));
        scrollbarWidget.orientation(ScrollbarWidget.Orientation.VERTICAL);
        scrollbarWidget.minValue(0);
        scrollbarWidget.maxValue(0);
        scrollbarWidget.visibleSize(MAX_LINES);
        scrollbarWidget.scrollStep(1.0);
        scrollbarWidget.onValueChanged(v -> refreshEffectButtons());
        scrollbarWidget.addScrollHoverArea(new ScreenCoordinate(listX, listY, listW, listH));
        addWidget(scrollbarWidget);

        // 效果列表按钮（先添加，操作按钮后添加以确保其悬浮提示绘制在列表上层）
        effectButtonWidgets.clear();
        int iconW = AbstractGuiUtils.ITEM_ICON_SIZE + 4;
        int textMaxW = listItemW - iconW - 4;
        for (int i = 0; i < MAX_LINES; i++) {
            ButtonWidget btn = new ButtonWidget(this);
            btn.id("effect_btn_" + i);
            btn.renderCoordinate(new ScreenCoordinate(listX, listY + i * (ROW_HEIGHT + ITEM_SPACING), listItemW, ROW_HEIGHT - 2));
            btn.text(Text.empty());
            btn.paddingLeft(iconW).paddingRight(4);
            btn.visible(false);

            EffectIconWidget iconWidget = new EffectIconWidget(this, new ScreenCoordinate(2, 2, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
            iconWidget.showText(false);
            iconWidget.enableTooltip(false);
            iconWidget.seasonTooltip(useSeasonTooltip);
            iconWidget.visible(false);
            btn.addChild(iconWidget);

            LabelWidget labelWidget = new LabelWidget(this, new ScreenCoordinate(iconW, 2, textMaxW, ROW_HEIGHT - 4));
            labelWidget.text(Text.empty());
            labelWidget.textWrap(false).textEllipsisPosition(EnumEllipsisPosition.MIDDLE).textVerticalAlign(EnumAlignment.CENTER);
            labelWidget.visible(false);
            btn.addChild(labelWidget);

            TooltipWidget itemTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, listItemW, ROW_HEIGHT - 2));
            itemTooltip.text(Text.empty());
            itemTooltip.seasonTooltip(useSeasonTooltip);
            itemTooltip.visible(false);
            btn.addChild(itemTooltip);

            btn.onClick(b -> {
                Object effectId = b.property("effectId");
                if (effectId instanceof String) {
                    handleEffect((String) effectId);
                    if (selectedEffectWidget != null) selectedEffectWidget.focused(false);
                    selectedEffectWidget = btn;
                }
            });

            effectButtonWidgets.add(btn);
            addWidget(btn);
        }

        // 确认与取消按钮
        ButtonWidget cancelButtonWidget = new ButtonWidget(this);
        cancelButtonWidget.id("cancel");
        cancelButtonWidget.renderCoordinate(new ScreenCoordinate(cancelX, btnY, btnW, BTN_H));
        cancelButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "cancel"));
        cancelButtonWidget.onClick(b -> Minecraft.getInstance().setScreen(args.parentScreen()));
        addWidget(cancelButtonWidget);

        ButtonWidget submitButtonWidget = new ButtonWidget(this);
        submitButtonWidget.id("submit");
        submitButtonWidget.renderCoordinate(new ScreenCoordinate(submitX, btnY, btnW, BTN_H));
        submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, "submit"));
        submitButtonWidget.onClick(b -> {
            if (this.currentEffect == null) {
                Minecraft.getInstance().setScreen(args.parentScreen());
            } else {
                EffectInstance effectInstance = EffectUtils.copyEffectInstance(this.currentEffect);
                if (args.onDataReceived1() != null) {
                    args.onDataReceived1().accept(effectInstance);
                    LOGGER.debug("Effect selected: {}", EffectUtils.serializeEffectInstance(effectInstance));
                    Minecraft.getInstance().setScreen(args.parentScreen());
                } else if (args.onDataReceived2() != null) {
                    String result = args.onDataReceived2().apply(effectInstance);
                    if (StringUtils.isNullOrEmpty(result)) {
                        LOGGER.debug("Effect selected: {}", EffectUtils.serializeEffectInstance(effectInstance));
                        Minecraft.getInstance().setScreen(args.parentScreen());
                    }
                }
            }
        });
        addWidget(submitButtonWidget);

        // 操作按钮（最后添加，确保悬浮提示绘制在列表项上层）
        int opBtnX = this.panelLeft - OP_BTN_SIZE - OP_BTN_GAP;
        int opBtnY = this.panelTop;
        String[] btnIds = {"type", "effect", "duration", "amplifier"};
        for (int i = 0; i < btnIds.length; i++) {
            String btnId = btnIds[i];
            ButtonWidget btn = new ButtonWidget(this);
            btn.id(btnId);
            btn.renderCoordinate(new ScreenCoordinate(opBtnX, opBtnY + i * (OP_BTN_SIZE + OP_BTN_GAP), OP_BTN_SIZE, OP_BTN_SIZE));
            btn.text(Text.empty());
            btn.paddingLeft(0).paddingRight(0).paddingTop(0).paddingBottom(0);

            int opCode = parseOperationButtonType(btnId);
            if (opCode == ButtonType.TYPE.code()) {
                typeButtonItemWidget = new ItemWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
                typeButtonItemWidget.itemStack(new net.minecraft.item.ItemStack(this.playerMode ? Items.CHEST : Items.COMPASS));
                typeButtonItemWidget.showCountText(false);
                typeButtonItemWidget.enableTooltip(false);
                typeTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, OP_BTN_SIZE, OP_BTN_SIZE));
                typeTooltip.seasonTooltip(useSeasonTooltip);
                typeTooltip.text(Text.transAuto(BaniraCodex.MODID,
                        (this.playerMode ? "effect_display_mode_player" : "effect_display_mode_all"),
                        (this.playerMode ? EffectUtils.getPlayerEffects().size() : EffectUtils.getAllEffects().size())));
                btn.addChild(typeTooltip);
                btn.addChild(typeButtonItemWidget);
            } else if (opCode == ButtonType.EFFECT.code()) {
                effectButtonIconWidget = new EffectIconWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
                effectButtonIconWidget.effectInstance(this.currentEffect);
                effectButtonIconWidget.showText(false);
                effectButtonIconWidget.enableTooltip(false);
                effectTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, OP_BTN_SIZE, OP_BTN_SIZE));
                effectTooltip.seasonTooltip(useSeasonTooltip);
                effectTooltip.text(Text.transAuto(BaniraCodex.MODID, "effect_select_effect"));
                btn.addChild(effectTooltip);
                btn.addChild(effectButtonIconWidget);
            } else if (opCode == ButtonType.DURATION.code()) {
                ItemWidget iconWidget = new ItemWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
                iconWidget.itemStack(new net.minecraft.item.ItemStack(Items.CLOCK));
                iconWidget.showCountText(false);
                iconWidget.enableTooltip(false);
                durationTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, OP_BTN_SIZE, OP_BTN_SIZE));
                durationTooltip.seasonTooltip(useSeasonTooltip);
                durationTooltip.text(Text.transAuto(BaniraCodex.MODID, "set_duration", this.currentEffect.getDuration()));
                btn.addChild(durationTooltip);
                btn.addChild(iconWidget);
            } else if (opCode == ButtonType.AMPLIFIER.code()) {
                ItemWidget iconWidget = new ItemWidget(this, new ScreenCoordinate(1, 1, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE));
                iconWidget.itemStack(new net.minecraft.item.ItemStack(Items.ANVIL));
                iconWidget.showCountText(false);
                iconWidget.enableTooltip(false);
                amplifierTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, OP_BTN_SIZE, OP_BTN_SIZE));
                amplifierTooltip.seasonTooltip(useSeasonTooltip);
                amplifierTooltip.text(Text.transAuto(BaniraCodex.MODID, "set_amplifier", NumberUtils.intToRoman(this.currentEffect.getAmplifier() + 1)));
                btn.addChild(amplifierTooltip);
                btn.addChild(iconWidget);
            }
            btn.onClick(b -> handleOperationInternal(opCode));
            addWidget(btn);
        }

        updateSearchResults();
    }

    @Override
    public void renderEvent(MatrixStack stack, float partialTicks) {
        ShapeDrawArgs panelBg = ShapeDrawArgs.rect(stack, panelLeft, panelTop, panelW, panelH, getEffectiveTheme().panelBg());
        panelBg.rect().radius(5).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(panelBg);

        if (selectedEffectWidget != null) selectedEffectWidget.focused(true);
        super.renderWidgets(stack, partialTicks);

        if (searchInputWidget != null) {
            this.inputFieldText = searchInputWidget.value();
        }

        refreshEffectButtons();
    }

    @Override
    public void mouseClickedEvent(MouseClickedHandleArgs eventArgs) {
        AtomicBoolean flag = new AtomicBoolean(false);
        if (inputState.isMousePressed(GLFWKey.GLFW_MOUSE_BUTTON_4)) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            flag.set(true);
        }
        eventArgs.consumed(flag.get());

        super.mouseClickedEvent(eventArgs);

        if (searchInputWidget != null && searchInputWidget.focused()) {
            BaseWidget inputBase = searchInputWidget;
            double mouseX = eventArgs.mouseX();
            double mouseY = eventArgs.mouseY();
            if (!inputBase.isMouseInside(mouseX, mouseY)) {
                this.unfocusWidget(searchInputWidget);
            }
        }
    }

    @Override
    public void keyPressedEvent(KeyPressedHandleArgs eventArgs) {
        super.keyPressedEvent(eventArgs);
        if (eventArgs.consumed()) return;

        if (super.inputState.isEscapePressed() ||
                (super.inputState.isBackspacePressed() &&
                        (searchInputWidget == null || !searchInputWidget.focused()))) {
            Minecraft.getInstance().setScreen(args.parentScreen());
            eventArgs.consumed(true);
        } else if (super.inputState.isEnterPressed() && searchInputWidget != null && searchInputWidget.focused()) {
            this.updateSearchResults();
            eventArgs.consumed(true);
        }
    }

    @Override
    protected void refreshWidget() {
        super.refreshWidget();
        updateSearchResults();
    }

    private void refreshEffectButtons() {
        if (effectButtonWidgets.isEmpty()) return;

        int scrollOffset = scrollbarWidget != null ? (int) scrollbarWidget.value() : 0;

        boolean found = false;
        for (int i = 0; i < effectButtonWidgets.size(); i++) {
            ButtonWidget buttonWidget = effectButtonWidgets.get(i);
            int index = scrollOffset + i;
            EffectIconWidget ew = buttonWidget.findChildByType(EffectIconWidget.class);
            LabelWidget lw = buttonWidget.findChildByType(LabelWidget.class);
            TooltipWidget tw = buttonWidget.findChildByType(TooltipWidget.class);
            if (ew == null || lw == null) continue;

            if (index >= 0 && index < effectList.size()) {
                Effect effect = effectList.get(index);
                String effectId = EffectUtils.getEffectRegistryString(effect);
                String displayName = EffectUtils.getEffectDisplayName(effect);
                EffectInstance instance = new EffectInstance(effect, currentEffect.getDuration(), currentEffect.getAmplifier());
                ew.effectInstance(instance);
                ew.visible(true);
                lw.text(Text.literal(displayName).color(Color.argb(getEffectiveTheme().listItemText())));
                lw.visible(true);
                if (tw != null) {
                    tw.text(Text.literal(buildTooltip(displayName, effectId)));
                    tw.visible(true);
                }
                buttonWidget.property("effectId", effectId);
                buttonWidget.visible(true);
                buttonWidget.enabled(true);
            } else {
                ew.effectInstance(null);
                ew.visible(false);
                lw.text(Text.empty());
                lw.visible(false);
                if (tw != null) {
                    tw.text(Text.empty());
                    tw.visible(false);
                }
                buttonWidget.property("effectId", "");
                buttonWidget.visible(false);
                buttonWidget.enabled(false);
            }

            if (selectedEffectWidget != null) selectedEffectWidget.focused(false);
            if (currentEffect != null && EffectUtils.getEffectRegistryString(currentEffect.getEffect()).equals(buttonWidget.property("effectId"))) {
                selectedEffectWidget = buttonWidget;
                found = true;
            }
        }
        if (!found) selectedEffectWidget = null;

        if (typeTooltip != null) {
            typeTooltip.text(Text.transAuto(BaniraCodex.MODID,
                    (this.playerMode ? "effect_display_mode_player" : "effect_display_mode_all"),
                    (this.playerMode ? EffectUtils.getPlayerEffects().size() : EffectUtils.getAllEffects().size())));
        }
        if (typeButtonItemWidget != null) {
            typeButtonItemWidget.itemStack(new ItemStack(this.playerMode ? Items.CHEST : Items.COMPASS));
        }
        if (effectButtonIconWidget != null) {
            effectButtonIconWidget.effectInstance(this.currentEffect);
        }
        if (effectTooltip != null) {
            effectTooltip.text(Text.literal(EffectUtils.getEffectDisplayName(currentEffect) + " "
                    + NumberUtils.intToRoman(currentEffect.getAmplifier() + 1)));
        }
        if (durationTooltip != null) {
            durationTooltip.text(Text.transAuto(BaniraCodex.MODID, "set_duration", currentEffect.getDuration()));
        }
        if (amplifierTooltip != null) {
            amplifierTooltip.text(Text.transAuto(BaniraCodex.MODID, "set_amplifier", NumberUtils.intToRoman(currentEffect.getAmplifier() + 1)));
        }
    }

    private static String buildTooltip(String displayName, String effectId) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotNullOrEmpty(displayName)) parts.add(displayName);
        if (StringUtils.isNotNullOrEmpty(effectId) && !effectId.equals(displayName)) {
            parts.add(effectId);
        }
        return String.join("\n", parts);
    }

    private void updateSearchResults() {
        String s = this.searchInputWidget != null ? this.searchInputWidget.value() : this.inputFieldText;
        this.effectList.clear();

        if (this.playerMode) {
            this.effectList.addAll(EffectUtils.getPlayerEffects());
            if (StringUtils.isNotNullOrEmpty(s)) {
                List<Effect> filtered = new ArrayList<>();
                String lower = s.trim().toLowerCase();
                for (Effect e : this.effectList) {
                    if (EffectUtils.getEffectRegistryString(e).toLowerCase().contains(lower)
                            || EffectUtils.getEffectDisplayName(e).toLowerCase().contains(lower)) {
                        filtered.add(e);
                    }
                }
                this.effectList.clear();
                this.effectList.addAll(filtered);
            }
        } else {
            this.effectList.addAll(EffectUtils.searchEffects(s));
        }

        if (scrollbarWidget != null) {
            scrollbarWidget.maxValue(Math.max(0, effectList.size() - MAX_LINES));
            if (scrollbarWidget.value() > scrollbarWidget.maxValue()) {
                scrollbarWidget.setValue(0);
            }
        }

        refreshEffectButtons();
        LOGGER.debug("Effect search results updated: count={}, query={}", effectList.size(), s);
    }

    private void handleEffect(String effectId) {
        if (StringUtils.isNotNullOrEmpty(effectId)) {
            Effect effect = EffectUtils.getEffectFromRegistry(effectId);
            if (effect != null) {
                this.currentEffect = new EffectInstance(effect, this.currentEffect.getDuration(), this.currentEffect.getAmplifier());
                LOGGER.debug("Select effect: {}", EffectUtils.getEffectDisplayName(effect));
                refreshEffectButtons();
            }
        }
    }

    private int parseOperationButtonType(String buttonId) {
        if ("type".equals(buttonId) || "1".equals(buttonId)) return ButtonType.TYPE.code();
        if ("effect".equals(buttonId) || "2".equals(buttonId)) return ButtonType.EFFECT.code();
        if ("duration".equals(buttonId) || "3".equals(buttonId)) return ButtonType.DURATION.code();
        if ("amplifier".equals(buttonId) || "4".equals(buttonId)) return ButtonType.AMPLIFIER.code();
        try {
            return Integer.parseInt(buttonId);
        } catch (NumberFormatException e) {
            LOGGER.debug("Unknown operation button id: {}", buttonId);
            return 0;
        }
    }

    private void handleOperationInternal(int operationCode) {
        if (operationCode == ButtonType.TYPE.code()) {
            this.playerMode = !this.playerMode;
            this.updateSearchResults();
        } else if (operationCode == ButtonType.EFFECT.code()) {
            StringInputScreen.Args inputArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.transAuto(BaniraCodex.MODID, "enter_effect_id"))
                            .hint(Text.transAuto(BaniraCodex.MODID, "enter_something"))
                            .defaultValue(EffectUtils.getEffectRegistryString(this.currentEffect))
                            .validator((input) -> {
                                Effect e = EffectUtils.getEffectFromRegistry(input.value());
                                if (e == null) {
                                    return Component.transClientAuto(BaniraCodex.MODID, "enter_effect_id_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        String id = input.firstValue();
                        Effect effect = EffectUtils.getEffectFromRegistry(id);
                        if (effect != null) {
                            this.currentEffect = new EffectInstance(effect, this.currentEffect.getDuration(), this.currentEffect.getAmplifier());
                        }
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(inputArgs));
        } else if (operationCode == ButtonType.DURATION.code()) {
            StringInputScreen.Args inputArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.transAuto(BaniraCodex.MODID, "enter_effect_duration"))
                            .regex("\\d{0,4}")
                            .defaultValue(String.valueOf(this.currentEffect.getDuration()))
                            .validator((input) -> {
                                int duration = NumberUtils.toInt(input.value());
                                if (duration <= 0 || duration > 60 * 60 * 24 * 30) {
                                    return Component.transClientAuto(BaniraCodex.MODID, "enter_effect_duration_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        int duration = NumberUtils.toInt(input.firstValue());
                        this.currentEffect = new EffectInstance(this.currentEffect.getEffect(), duration, this.currentEffect.getAmplifier());
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(inputArgs));
        } else if (operationCode == ButtonType.AMPLIFIER.code()) {
            StringInputScreen.Args inputArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .title(Text.transAuto(BaniraCodex.MODID, "enter_effect_amplifier"))
                            .regex("\\d{0,3}")
                            .defaultValue(String.valueOf(this.currentEffect.getAmplifier() + 1))
                            .validator((input) -> {
                                int amplifier = NumberUtils.toInt(input.value());
                                if (amplifier <= 0 || amplifier > 100) {
                                    return Component.transClientAuto(BaniraCodex.MODID, "enter_effect_amplifier_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .setCallback(input -> {
                        int amplifier = NumberUtils.toInt(input.firstValue());
                        this.currentEffect = new EffectInstance(this.currentEffect.getEffect(), this.currentEffect.getDuration(), amplifier - 1);
                    });
            Minecraft.getInstance().setScreen(new StringInputScreen(inputArgs));
        }
    }
}
