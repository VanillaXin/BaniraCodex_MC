package xin.vanilla.banira.internal.client.ui.page;

import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.*;
import xin.vanilla.banira.common.enums.*;
import xin.vanilla.banira.internal.client.ui.*;
import java.util.*;
import java.util.function.Consumer;

public final class DebugUiSession implements UiPageSession {
    private static final Map<String, String> ACTIONS;
    static {
        Map<String, String> actions = new LinkedHashMap<>();
        actions.put("notification", "\u901a\u77e5"); actions.put("input", "\u8f93\u5165\u6846"); actions.put("input_large", "\u591a\u5b57\u6bb5\u8868\u5355");
        actions.put("item", "\u7269\u54c1"); actions.put("effect", "\u6548\u679c"); actions.put("advancement", "\u8fdb\u5ea6"); actions.put("notification_log", "\u901a\u77e5\u8bb0\u5f55");
        actions.put("config_client", "\u5ba2\u6237\u7aef\u914d\u7f6e"); actions.put("config_common", "\u901a\u7528\u914d\u7f6e"); actions.put("config_test", "\u6309\u94ae\u957f\u6309\u6d4b\u8bd5");
        actions.put("quick_plus", "\u5feb\u6377\u5165\u53e3\u9879+1"); actions.put("quick_minus", "\u5feb\u6377\u5165\u53e3\u9879-1");
        actions.put("opt_a", "\u9009\u9879 A"); actions.put("opt_b", "\u9009\u9879 B");
        for (ButtonWidget.PresetStyle style : presets()) actions.put("preset_" + style.name(), style.name());
        ACTIONS = Collections.unmodifiableMap(actions);
    }
    private final Consumer<String> action;
    private final Map<String, String> settings = new LinkedHashMap<>();
    private final Map<String, List<String>> selected = new HashMap<>();
    private BaniraScreen nativeScreen;
    private UiPageSpec spec;
    private UiView view;
    private UiOwnedResources bindings = new UiOwnedResources();
    private boolean closed;
    private String content = "";
    private int pressed = -1;
    public DebugUiSession(Consumer<String> action) {
        this.action = Objects.requireNonNull(action); settings.put("lines", "2"); settings.put("length", "20"); settings.put("font", "9"); settings.put("wrap", "false");
        selected.put("season", Collections.singletonList(EnumSeason.AUTO.name())); selected.put("position", Collections.singletonList(EnumPosition.TOP_RIGHT.name()));
    }
    public Set<String> actions() { return ACTIONS.keySet(); }
    public static ButtonWidget.PresetStyle[] presets() { return new ButtonWidget.PresetStyle[]{ButtonWidget.PresetStyle.CLOSE, ButtonWidget.PresetStyle.MINUS, ButtonWidget.PresetStyle.PLUS, ButtonWidget.PresetStyle.MAXIMIZE, ButtonWidget.PresetStyle.ARROW_UP, ButtonWidget.PresetStyle.ARROW_DOWN, ButtonWidget.PresetStyle.ARROW_LEFT, ButtonWidget.PresetStyle.ARROW_RIGHT}; }
    public String setting(String id) { return settings.get(id); }
    public boolean setting(String id, String text) {
        if (closed || !settings.containsKey(id)) return false;
        if (id.equals("wrap")) { if (!text.equals("true") && !text.equals("false")) return false; }
        else {
            try { int value = Integer.parseInt(text); int max = id.equals("length") ? 256 : 32, min = id.equals("font") ? 4 : id.equals("length") ? 2 : 1;
                if (value < min || value > max) return false;
            } catch (NumberFormatException invalid) { return false; }
        }
        settings.put(id, text); updateDiagnostic(); return true;
    }
    public String content() { return content; }
    public void content(String content) { if (!closed) { this.content = content; updateDiagnostic(); } }
    public List<String> selected(String id) { return new ArrayList<>(selected.getOrDefault(id, Collections.emptyList())); }
    public void selected(String id, List<String> values) {
        if (closed) return;
        selected.put(id, new ArrayList<>(values));
        if (id.equals("season") && nativeScreen != null) nativeScreen.season(season());
        updateDiagnostic();
    }
    public EnumSeason season() { List<String> values = selected("season"); return values.isEmpty() ? EnumSeason.AUTO : EnumSeason.valueOfDefault(values.get(0)); }
    public EnumPosition position() { List<String> values = selected("position"); return values.isEmpty() ? EnumPosition.TOP_RIGHT : EnumPosition.valueOf(values.get(0)); }
    public void attachNative(BaniraScreen screen) { nativeScreen = screen; }
    public UiPageSpec spec() {
        if (spec == null) spec = new UiPageSpec("banira_codex:debug", "banira_codex/ui/debug.html", this, previous -> nativeScreen, nativeScreen::getEffectiveTheme);
        return spec;
    }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD); if (closed) return;
        this.view = Objects.requireNonNull(view); bindings = new UiOwnedResources();
        if (view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof BaniraScreen) return;
        view.text("title", BaniraComponent.get().literal("Banira Debug")); view.text("close", BaniraComponent.get().transClientAuto("cancel"));
        bindings.own(view.onAction("close", () -> { if (this.view == view) view.requestClose(); }));
        ACTIONS.forEach((id, label) -> {
            view.text(id, BaniraComponent.get().literal(id.startsWith("preset_") ? "" : label)); view.tooltip(id, BaniraComponent.get().literal(label));
            if (id.startsWith("preset_")) view.gameElement(id, UiGameElement.preset(ButtonWidget.PresetStyle.valueOf(id.substring(7))));
            if (!id.equals("config_test")) bindings.own(view.onAction(id, () -> { if (this.view == view) action.accept(id); }));
        });
        bindings.own(view.onLongAction("config_test", 1000, () -> { if (this.view == view) action.accept("config_test"); }));
        bindings.own(view.onLongAction("config_client", 1000, () -> { if (this.view == view) action.accept("config_common"); }));
        for (String id : settings.keySet()) {
            view.control(id, new UiControlSpec(id.equals("wrap") ? UiControlSpec.Kind.SELECT : UiControlSpec.Kind.DECIMAL, 3, "[0-9]*", false,
                    id.equals("wrap") ? Arrays.asList(new DropdownOption("false", "false"), new DropdownOption("true", "true")) : Collections.emptyList(), DropdownInputMode.SELECTION_ONLY));
            if (id.equals("wrap")) { view.value(id, UiValue.text(setting(id))); bindings.own(view.onValue(id, value -> { if (this.view == view && setting(id, value.asText())) action.accept("regenerate"); })); }
            else { view.draft(id, setting(id)); bindings.own(view.onDraft(id, text -> { if (this.view == view && setting(id, text)) action.accept("regenerate"); })); }
        }
        select(view, "season", enumOptions(EnumSeason.values()), false);
        select(view, "position", enumOptions(EnumPosition.values()), false);
        select(view, "fruit", options("\u82f9\u679c", "\u9999\u8549", "\u6a59\u5b50", "\u8461\u8404", "\u8349\u8393", "\u897f\u74dc"), true);
        select(view, "vegetable", options(vegetables()), true);
        List<DropdownOption> menu = new ArrayList<>();
        for (String id : Arrays.asList("opt_a", "opt_b", "notification", "input", "item", "advancement", "effect", "notification_log")) {
            menu.add(new DropdownOption(id, ACTIONS.get(id)));
        }
        view.control("popup", new UiControlSpec(UiControlSpec.Kind.SELECT, 0, "", false, menu, DropdownInputMode.SELECTION_ONLY));
        view.value("popup", UiValue.text("")); view.tooltip("popup", BaniraComponent.get().literal("PopupOption"));
        bindings.own(view.onValue("popup", value -> {
            if (this.view == view && ACTIONS.containsKey(value.asText())) { action.accept(value.asText()); view.value("popup", UiValue.text("")); }
        }));
        bindings.own(view.onPointer("diagnostic", event -> {
            if (this.view != view) return;
            if (event.kind() == UiPointer.Kind.DOWN) pressed = event.button();
            else if (event.kind() == UiPointer.Kind.UP) pressed = -1;
            updateDiagnostic();
        }));
        updateDiagnostic();
    }
    private static List<DropdownOption> enumOptions(Enum<?>[] values) {
        List<DropdownOption> result = new ArrayList<>(); for (Enum<?> value : values) result.add(new DropdownOption(value.name(), value instanceof IEnumDescribable ? ((IEnumDescribable) value).enumDescription().toString() : value.name())); return result;
    }
    private static List<DropdownOption> options(String... values) { List<DropdownOption> result = new ArrayList<>(); for (String value : values) result.add(new DropdownOption(value, value)); return result; }
    private void select(UiView view, String id, List<DropdownOption> options, boolean multi) {
        view.control(id, new UiControlSpec(UiControlSpec.Kind.SELECT, 0, "", multi, options, DropdownInputMode.SELECTION_ONLY));
        view.value(id, multi ? UiValue.strings(selected(id)) : UiValue.text(selected(id).isEmpty() ? "" : selected(id).get(0)));
        bindings.own(view.onValue(id, value -> { if (this.view == view) selected(id, multi ? value.asStrings() : Collections.singletonList(value.asText())); }));
    }
    private void updateDiagnostic() {
        if (view != null && !closed) view.gameElement("diagnostic", UiGameElement.diagnostic(content, Integer.parseInt(setting("font")), Boolean.parseBoolean(setting("wrap")), season(), pressed));
    }
    public static String[] vegetables() {
        return ("\u767d\u83dc,\u83e0\u83dc,\u6cb9\u83dc,\u751f\u83dc,\u7a7a\u5fc3\u83dc,\u97ed\u83dc,\u82b9\u83dc,\u9999\u83dc,\u833c\u84bf,\u82cb\u83dc,"
                + "\u82a5\u84dd,\u5c0f\u767d\u83dc,\u5927\u767d\u83dc,\u5377\u5fc3\u83dc,\u7d2b\u7518\u84dd,\u7fbd\u8863\u7518\u84dd,\u897f\u5170\u82b1,\u83dc\u82b1,\u82a5\u83dc,\u96ea\u91cc\u7ea2,"
                + "\u841d\u535c,\u80e1\u841d\u535c,\u767d\u841d\u535c,\u9752\u841d\u535c,\u6a31\u6843\u841d\u535c,\u571f\u8c46,\u7ea2\u85af,\u7d2b\u85af,\u5c71\u836f,\u828b\u5934,"
                + "\u6d0b\u8471,\u5927\u849c,\u849c\u82d7,\u849c\u9ec4,\u5927\u8471,\u5c0f\u8471,\u97ed\u83dc\u82d4,\u82a6\u7b0b,\u7af9\u7b0b,\u83b4\u7b0b,"
                + "\u83b2\u85d5,\u83f1\u89d2,\u8378\u8360,\u6148\u59d1,\u767e\u5408,\u751f\u59dc,\u6d0b\u59dc,\u8c46\u82bd,\u7eff\u8c46\u82bd,\u9ec4\u8c46\u82bd,"
                + "\u8c47\u8c46,\u56db\u5b63\u8c46,\u6241\u8c46,\u8377\u5170\u8c46,\u6bdb\u8c46,\u8695\u8c46,\u8c4c\u8c46,\u9ec4\u8c46,\u7eff\u8c46,\u7ea2\u8c46,"
                + "\u9ec4\u74dc,\u51ac\u74dc,\u5357\u74dc,\u82e6\u74dc,\u4e1d\u74dc,\u897f\u846b\u82a6,\u4f5b\u624b\u74dc,\u6728\u74dc,\u897f\u7ea2\u67ff,\u8304\u5b50,"
                + "\u8fa3\u6912,\u9752\u6912,\u5f69\u6912,\u751c\u6912,\u79cb\u8475,\u7389\u7c73,\u9c9c\u7389\u7c73,\u8334\u9999,\u841d\u535c\u82d7,\u8c4c\u8c46\u82d7,"
                + "\u9ed1\u6728\u8033,\u94f6\u8033,\u9999\u83c7,\u91d1\u9488\u83c7,\u5e73\u83c7,\u674f\u9c8d\u83c7,\u53e3\u8611,\u7334\u5934\u83c7,\u8336\u6811\u83c7,\u7af9\u836a,"
                + "\u6d77\u5e26,\u7d2b\u83dc,\u88d9\u5e26\u83dc,\u77f3\u82b1\u83dc,\u5730\u76ae\u83dc,\u8568\u83dc,\u9a6c\u9f7f\u82cb,\u8360\u83dc,\u84b2\u516c\u82f1,\u9c7c\u8165\u8349").split(",");
    }
    public static void drawDiagnostics(GuiGraphics graphics, ScreenCoordinate bounds, UiGameElement element, BaniraColorConfig theme) {
        float x = (float) (bounds.x() + bounds.width() / 2), y = (float) (bounds.y() + bounds.height() / 2);
        BaseShapeWidget.drawShape(ShapeDrawArgs.circle(graphics.pose(), x, y, 15, 0x4487CEEB));
        ShapeDrawArgs ellipse = ShapeDrawArgs.ellipse(graphics.pose(), x, y, 7.5f, 15, 0x4487CEEB);
        ellipse.ellipse().rotation((System.currentTimeMillis() / 50d) % 360).border(2); BaseShapeWidget.drawShape(ellipse);
        ShapeDrawArgs rect = ShapeDrawArgs.rect(graphics.pose(), x - 30, y - 30, 60, 60, 0x44B8D4F0);
        rect.rect().topRight(15).bottomLeft(15).bottomRight(15); BaseShapeWidget.drawShape(rect);
        ShapeDrawArgs border = ShapeDrawArgs.rect(graphics.pose(), x - 35, y - 35, 70, 70, 0x44B8D4F0);
        border.rect().border(4).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE).topLeft(4).topRight(35).bottomLeft(35).bottomRight(16); BaseShapeWidget.drawShape(border);
        Texture[] skin = PlayerSkinTextureUtils.headFaceTextures(xin.vanilla.banira.common.util.PlayerUtils.getPlayerUUID());
        if (skin != null) for (Texture layer : skin) ImageWidget.blitBlend(graphics.pose(), layer, (int) x - 8, (int) y - 8, 16, 16);
        if (element.pressed() < 0 || theme == null) return;
        xin.vanilla.banira.common.data.KeyValue<Integer, Integer> mouse = xin.vanilla.banira.internal.client.InputStateManager.getGuiCursorPos();
        if (mouse.key() < bounds.x() || mouse.key() >= bounds.x() + bounds.width() || mouse.val() < bounds.y() || mouse.val() >= bounds.y() + bounds.height()) return;
        FontDrawArgs args = FontDrawArgs.ofPopo(Text.literal(element.text()).stack(graphics.pose()).font(net.minecraft.client.Minecraft.getInstance().font))
                .x(mouse.key()).y(mouse.val()).fontSize(element.fontSize()).align(EnumAlignment.CENTER).wrap(element.wrap()).maxWidth(element.wrap() ? Math.max(1, (int) bounds.width() / 2) : 0);
        if (element.pressed() == 0) TooltipWidget.drawPopupMessage(graphics.pose(), args.popupUseTexture(false), theme, element.season());
        else TooltipWidget.drawPopupMessageWithSeasonTexture(graphics.pose(), args, element.season());
    }
    @Override public void detach(UiDetachReason reason) { view = null; pressed = -1; bindings.close(); }
    @Override public void close(UiCloseReason reason) { closed = true; detach(UiDetachReason.SWITCH); }
}
