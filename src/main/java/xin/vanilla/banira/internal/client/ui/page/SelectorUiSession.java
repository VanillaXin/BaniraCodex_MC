package xin.vanilla.banira.internal.client.ui.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.client.ui.*;
import xin.vanilla.banira.internal.network.data.AdvancementData;
import java.util.*;
import java.util.function.*;

public final class SelectorUiSession implements UiPageSession {
    public enum Kind { ITEM, EFFECT, ADVANCEMENT }
    private final Kind kind;
    private final Screen parent;
    private final Function<Object, String> callback;
    private final Supplier<Boolean> shouldClose;
    private final boolean closesAfterSubmit;
    private ItemStack item;
    private MobEffectInstance effect;
    private ResourceLocation advancement;
    private String query = "", error = "";
    private boolean mode, closed, submitted, submitting, loading;
    private long version;
    private int resultCount;
    private BaniraScreen nativeScreen;
    private UiPageSpec spec;
    private UiView view;
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rowBindings = new IdentityHashMap<>();
    private final Map<String, FormUiSession> editors = new HashMap<>();

    public SelectorUiSession(ItemSelectScreen.Args args) {
        args.validate(); kind = Kind.ITEM; parent = args.parentScreen(); shouldClose = args.shouldClose(); closesAfterSubmit = args.closeAfterSubmit();
        item = copyItem(args.defaultItem());
        callback = value -> { if (args.onDataReceived1() != null) { args.onDataReceived1().accept((ItemStack) value); return ""; }
            return args.onDataReceived2().apply((ItemStack) value); };
    }
    public SelectorUiSession(EffectSelectScreen.Args args) {
        args.validate(); kind = Kind.EFFECT; parent = args.parentScreen(); shouldClose = args.shouldClose(); closesAfterSubmit = args.closeAfterSubmit();
        effect = args.defaultEffect() == null ? new MobEffectInstance(MobEffects.LUCK, 600, 0) : EffectUtils.copyEffectInstance(args.defaultEffect());
        callback = value -> { if (args.onDataReceived1() != null) { args.onDataReceived1().accept((MobEffectInstance) value); return ""; }
            return args.onDataReceived2().apply((MobEffectInstance) value); };
    }
    public SelectorUiSession(AdvancementSelectScreen.Args args) {
        args.validate(); kind = Kind.ADVANCEMENT; parent = args.parentScreen(); shouldClose = args.shouldClose(); closesAfterSubmit = args.closeAfterSubmit();
        advancement = args.defaultAdvancement(); mode = true;
        callback = value -> { if (args.onDataReceived1() != null) { args.onDataReceived1().accept((ResourceLocation) value); return ""; }
            return args.onDataReceived2().apply((ResourceLocation) value); };
    }
    public void attachNative(BaniraScreen screen) { nativeScreen = screen; }
    public UiPageSpec spec() {
        if (spec == null) spec = new UiPageSpec("banira_codex:" + surface(), "banira_codex/ui/" + surface() + ".html",
                this, previous -> nativeScreen, nativeScreen::getEffectiveTheme);
        return spec;
    }
    public String surface() { return kind.name().toLowerCase(Locale.ROOT) + "-select"; }
    private static ItemStack copyItem(ItemStack value) { return value == null || value.isEmpty() ? new ItemStack(Items.AIR) : value.copy(); }
    public ItemStack item() { return copyItem(item); }
    public MobEffectInstance effect() { return effect == null ? null : EffectUtils.copyEffectInstance(effect); }
    public ResourceLocation advancement() { return advancement; }
    public long version() { return version; }
    public String query() { return query; }
    public boolean mode() { return mode; }
    public String error() { return error; }
    public boolean closesAfterSubmit() { return closesAfterSubmit; }
    public void query(String text) {
        if (closed || submitted || Objects.equals(query, text)) return;
        query = text == null ? "" : text; version++; rebuild(); updateSelection();
    }
    public void mode(boolean value) {
        if (closed || submitted || mode == value) return;
        mode = value; version++; rebuild(); updateSelection();
    }
    public String selectedKey() {
        return kind == Kind.ITEM ? ItemUtils.serializeItemStack(item()) : kind == Kind.EFFECT ? EffectUtils.getEffectRegistryString(effect) : Objects.toString(advancement, "");
    }
    public void select(String id) {
        if (closed || submitted || submitting) return;
        if (kind == Kind.ITEM) { item = copyItem(ItemUtils.deserializeItemStack(id)); item.setCount(1); }
        else if (kind == Kind.EFFECT) {
            MobEffect value = EffectUtils.getEffectFromRegistry(id);
            if (value == null) return;
            effect = new MobEffectInstance(value, effect.getDuration(), effect.getAmplifier());
        } else {
            ResourceLocation parsed = Identifier.id().parse(id);
            if (parsed == null) return;
            advancement = parsed;
        }
        version++; updateSelection();
    }
    public boolean submit() {
        if (closed || submitted || submitting) return false;
        submitting = true;
        try {
            if (emptySelection()) { submitted = true; return true; }
            error = Objects.toString(callback.apply(kind == Kind.ITEM ? item() : kind == Kind.EFFECT ? effect() : advancement), "");
            submitted = error.isEmpty(); return submitted;
        } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), failure.getClass().getSimpleName()); return false; }
        finally { submitting = false; updateSelection(); }
    }
    private boolean emptySelection() { return kind == Kind.ITEM && item.isEmpty() || kind == Kind.ADVANCEMENT && advancement == null; }
    public void submitNative() {
        if (submit() && (closesAfterSubmit || emptySelection())) {
            if (!CommonUiPages.close(nativeScreen, UiCloseReason.SUBMIT)) { close(UiCloseReason.SUBMIT); Minecraft.getInstance().setScreen(parent); }
        }
    }
    private Screen editorParent() { return nativeScreen == null ? parent : nativeScreen; }
    public FormUiSession editor(String operation) {
        if (closed || submitted) throw new IllegalStateException("Selector is closed");
        FormUiSession existing = editors.get(operation);
        if (existing != null && !existing.finished()) return existing;
        InputFormScreen.Widget widget = new InputFormScreen.Widget();
        Consumer<InputFormScreen.Results> apply;
        if (kind == Kind.ITEM && operation.equals("item")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_item_id")).defaultValue(ItemUtils.getItemRegistryString(item))
                    .validator(input -> ItemUtils.getItemFromRegistry(input.value()) == null ? failure("enter_item_id_error", input.value()) : null);
            apply = input -> { ItemStack next = copyItem(ItemUtils.deserializeItemStack(input.firstValue(), ItemUtils.serializeItemStackTag(item))); next.setCount(item.getCount()); item = copyItem(next); };
        } else if (kind == Kind.ITEM && operation.equals("count")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_item_quantity")).regex("\\d{0,4}").defaultValue(Integer.toString(item.getCount()))
                    .validator(input -> NumberUtils.toInt(input.value()) <= 0 || NumberUtils.toInt(input.value()) > 64 * 9 * 5 ? failure("enter_item_quantity_error", input.value()) : null);
            apply = input -> { ItemStack next = item(); next.setCount(NumberUtils.toInt(input.firstValue())); item = next; };
        } else if (kind == Kind.ITEM && operation.equals("nbt")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_item_nbt")).defaultValue(ItemUtils.serializeItemStackTag(item)).validator(input -> {
                try { if (ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(item.getItem()) + input.value()).hasTag()) return null; }
                catch (Exception invalid) { }
                return failure("enter_item_nbt_error", input.value());
            });
            apply = input -> { ItemStack next = ItemUtils.deserializeItemStack(ItemUtils.getItemRegistry(item.getItem()) + input.firstValue()); next.setCount(item.getCount()); item = next; };
        } else if (kind == Kind.EFFECT && operation.equals("effect")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_effect_id")).defaultValue(EffectUtils.getEffectRegistryString(effect))
                    .validator(input -> EffectUtils.getEffectFromRegistry(input.value()) == null ? failure("enter_effect_id_error", input.value()) : null);
            apply = input -> effect = new MobEffectInstance(EffectUtils.getEffectFromRegistry(input.firstValue()), effect.getDuration(), effect.getAmplifier());
        } else if (kind == Kind.EFFECT && operation.equals("duration")) {
            int currentTicks = effect.getDuration();
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_effect_duration")).maxLength(EffectDurationModel.inputDigits(currentTicks))
                    .regex("[0-9]{0," + EffectDurationModel.inputDigits(currentTicks) + "}").defaultValue(Integer.toString(currentTicks)).validator(input -> {
                        try { EffectDurationModel.parseTicks(input.value(), currentTicks); return null; }
                        catch (IllegalArgumentException invalid) { return failure("enter_effect_duration_error", input.value()); }
                    });
            apply = input -> effect = new MobEffectInstance(effect.getEffect(), EffectDurationModel.parseTicks(input.firstValue(), currentTicks), effect.getAmplifier());
        } else if (kind == Kind.EFFECT && operation.equals("amplifier")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_effect_amplifier")).regex("\\d{0,3}").defaultValue(Integer.toString(effect.getAmplifier() + 1))
                    .validator(input -> NumberUtils.toInt(input.value()) <= 0 || NumberUtils.toInt(input.value()) > 100 ? failure("enter_effect_amplifier_error", input.value()) : null);
            apply = input -> effect = new MobEffectInstance(effect.getEffect(), effect.getDuration(), NumberUtils.toInt(input.firstValue()) - 1);
        } else if (kind == Kind.ADVANCEMENT && operation.equals("advancement")) {
            widget.title(Text.transAuto(Banira.MOD_ID, "enter_advancement_id")).defaultValue(Objects.toString(advancement, "")).validator(input -> {
                try { return Identifier.id().parse(input.value()) == null ? failure("enter_advancement_id_error", input.value()) : null; }
                catch (IllegalArgumentException invalid) { return failure("enter_advancement_id_error", input.value()); }
            });
            apply = input -> advancement = Identifier.id().parse(input.firstValue());
        } else throw new IllegalArgumentException("Unsupported selector editor");
        FormUiSession form = new FormUiSession(new InputFormScreen.Args().setParentScreen(editorParent()).addWidget(widget).setCallback(input -> {
            if (closed || submitted) { input.runningResult(new IllegalStateException("Selector is closed")); return; }
            apply.accept(input); version++; rebuild(); updateSelection();
        }));
        editors.put(operation, form); return form;
    }
    public void openEditor(String operation) { Minecraft.getInstance().setScreen(new InputFormScreen(editor(operation))); }
    private static String failure(String key, String input) { return BaniraComponent.get().transClientAuto(key, input).toString(); }
    private static Component tr(String key, Object... args) {
        return args.length == 0 ? BaniraComponent.get().transClientAuto(key) : BaniraComponent.get().transClientAuto(key, args);
    }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD); if (closed) return;
        this.view = Objects.requireNonNull(view); bindings = new UiOwnedResources();
        if (view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof BaniraScreen) return;
        view.text("title", tr("search_" + kind.name().toLowerCase(Locale.ROOT)));
        view.text("submit", tr("submit")); view.text("cancel", tr("cancel"));
        view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, 32767, "", false)); view.draft("search", query);
        bindings.own(view.onDraft("search", text -> { if (this.view == view) query(text); }));
        bindings.own(view.onAction("cancel", () -> { if (this.view == view) view.requestClose(); }));
        bindings.own(view.onAction("submit", () -> { if (this.view == view && submit() && (closesAfterSubmit || emptySelection())) view.requestClose(); }));
        bindings.own(view.onAction("mode", () -> { if (this.view == view) mode(!mode); }));
        for (String operation : operations()) {
            view.tooltip(operation, tr("enter_" + (operation.equals("item") || operation.equals("effect") || operation.equals("advancement") ? operation + "_id"
                    : operation.equals("count") ? "item_quantity" : operation.equals("nbt") ? "item_nbt" : "effect_" + operation)));
            view.text(operation, BaniraComponent.get().literal(operation.equals("count") ? "\u00d7" : operation.equals("nbt") ? "{}" : operation.equals("duration") ? "\u25f7" : operation.equals("amplifier") ? "+" : "#"));
            bindings.own(view.onAction(operation, () -> { if (this.view == view) openEditor(operation); }));
        }
        rebuild(); updateSelection();
    }
    private String[] operations() {
        return kind == Kind.ITEM ? new String[]{"item", "count", "nbt"} : kind == Kind.EFFECT ? new String[]{"effect", "duration", "amplifier"} : new String[]{"advancement"};
    }
    private static final class Row {
        final String id, title, tooltip; final UiGameElement icon;
        String key;
        Row(String id, String title, String tooltip, UiGameElement icon) { this.id = id; this.title = title; this.tooltip = tooltip; this.icon = icon; }
    }
    private List<Row> results() {
        List<Row> result = new ArrayList<>();
        if (kind == Kind.ITEM) {
            Collection<ItemStack> items = mode && Minecraft.getInstance() != null && Minecraft.getInstance().player != null
                    ? ItemUtils.searchPlayerItems(Minecraft.getInstance().player, query) : ItemUtils.searchItems(query);
            for (ItemStack stack : items) {
                String id = ItemUtils.serializeItemStack(stack), title = ItemUtils.getItemHoverNameString(stack);
                result.add(new Row(id, title, title + "\n" + id, UiGameElement.option(new DropdownOption(id, stack.copy(), null))));
            }
        } else if (kind == Kind.EFFECT) {
            Collection<MobEffect> effects = mode ? EffectUtils.getPlayerEffects() : EffectUtils.searchEffects(query);
            String lower = query.trim().toLowerCase(Locale.ROOT);
            for (MobEffect value : effects) {
                String id = EffectUtils.getEffectRegistryString(value), title = EffectUtils.getEffectDisplayName(value);
                if (mode && !id.toLowerCase(Locale.ROOT).contains(lower) && !title.toLowerCase(Locale.ROOT).contains(lower)) continue;
                result.add(new Row(id, title, title + "\n" + id, UiGameElement.effect(new MobEffectInstance(value, effect.getDuration(), effect.getAmplifier()))));
            }
        } else {
            AdvancementUtils.ensureAdvancementData(); loading = AdvancementUtils.isLoading();
            Collection<AdvancementData> values = mode ? AdvancementUtils.searchDisplayableAdvancements(query) : AdvancementUtils.searchAdvancements(query);
            for (AdvancementData value : values) {
                String id = value.id().toString(), title = value.displayInfo().getTitle().getString();
                String tooltip = String.join("\n", new LinkedHashSet<>(Arrays.asList(title, value.displayInfo().getDescription().getString(), id)));
                result.add(new Row(id, title, tooltip, UiGameElement.option(new DropdownOption(id, value.displayInfo().getIcon().copy(), null))));
            }
        }
        Map<String, Integer> occurrences = new HashMap<>();
        for (Row row : result) row.key = row.id + "#" + occurrences.merge(row.id, 1, Integer::sum);
        return result;
    }
    private void releaseRows() { rowBindings.values().forEach(UiOwnedResources::close); rowBindings.clear(); }
    private void rebuild() {
        if (view == null || closed || view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof BaniraScreen) return;
        releaseRows(); final UiView root = view; List<Row> rows = results(); resultCount = rows.size();
        view.rows("fields", new UiRowProvider() {
            public int size() { return rows.size(); }
            public String key(int index) { return rows.get(index).key; }
            public void bind(UiView row, int index) {
                if (view != root) return;
                UiOwnedResources old = rowBindings.remove(row); if (old != null) old.close();
                if (rowBindings.size() >= 120) { UiView first = rowBindings.keySet().iterator().next(); rowBindings.remove(first).close(); }
                UiOwnedResources owned = new UiOwnedResources(); rowBindings.put(row, owned); Row value = rows.get(index);
                row.text("open", BaniraComponent.get().literal(value.title)); row.tooltip("open", BaniraComponent.get().literal(value.tooltip));
                row.gameElement("icon", value.icon); row.text("label", BaniraComponent.get().literal(value.id));
                owned.own(row.onAction("open", () -> { if (view == root) select(value.id); }));
            }
        }, 120);
    }
    private void updateSelection() {
        if (view == null || closed) return;
        if (view instanceof NativeUiView) ((NativeUiView) view).state().selectedId(selectedKey());
        String description = selectedKey() + (kind == Kind.ITEM ? " \u00d7" + item.getCount()
                : kind == Kind.EFFECT ? " " + NumberUtils.intToRoman(effect.getAmplifier() + 1) + " " + EffectDurationModel.ticksText(effect.getDuration()) : "");
        view.text("selection", BaniraComponent.get().literal(description)); view.tooltip("selection", BaniraComponent.get().literal(description));
        view.text("error", BaniraComponent.get().literal(error)); view.enabled("submit", !submitted && !submitting);
        if (kind == Kind.ITEM) {
            view.gameElement("selected_icon", UiGameElement.option(new DropdownOption("selected", item(), null)));
            view.tooltip("count", tr("set_quantity", item.getCount()));
        } else if (kind == Kind.EFFECT) {
            view.gameElement("selected_icon", UiGameElement.effect(effect()));
            view.tooltip("duration", tr("set_duration", effect.getDuration())); view.tooltip("amplifier", tr("set_amplifier", NumberUtils.intToRoman(effect.getAmplifier() + 1)));
        } else {
            ItemStack icon = ItemStack.EMPTY;
            for (AdvancementData data : AdvancementUtils.getAllAdvancements()) if (data.id().equals(advancement)) { icon = data.displayInfo().getIcon().copy(); break; }
            view.gameElement("selected_icon", UiGameElement.option(new DropdownOption("selected", icon, null)));
        }
        view.text("mode", BaniraComponent.get().literal("\u21c4"));
        view.tooltip("mode", tr(kind == Kind.ITEM ? mode ? "item_display_mode_inventory" : "item_display_mode_all"
                : kind == Kind.EFFECT ? mode ? "effect_display_mode_player" : "effect_display_mode_all"
                : mode ? "advancement_display_mode_icon" : "advancement_display_mode_all", resultCount));
    }
    @Override public void tick() {
        if (closed) return;
        if (view != null && shouldClose != null && Boolean.TRUE.equals(shouldClose.get())) view.requestClose();
        else if (kind == Kind.ADVANCEMENT && view != null && loading && !AdvancementUtils.isLoading()) rebuild();
    }
    @Override public void detach(UiDetachReason reason) { view = null; bindings.close(); releaseRows(); }
    @Override public void close(UiCloseReason reason) { closed = true; detach(UiDetachReason.SWITCH); editors.values().forEach(form -> form.close(reason)); }
}
