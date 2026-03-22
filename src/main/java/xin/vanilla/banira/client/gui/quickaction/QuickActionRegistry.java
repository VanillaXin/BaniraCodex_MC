package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * 背包界面快捷操作注册器。仅客户端使用。
 * <p>
 * 注册为 {@link EnumQuickActionDisplay#ICON} 的项显示在图标组；
 * {@link EnumQuickActionDisplay#LIST_ONLY} 仅出现在「菜单锚点」图标的右键下拉列表中。
 * </p>
 * <p>图标可使用 {@link ItemStack}、{@link Item}、{@link Effect} 或 {@link ResourceLocation}（纹理），见各类 {@code register*} 重载。</p>
 * <p>可选在注册时附加 {@link QuickActionContextMenuItem}，在<strong>右键该托盘图标</strong>时与「隐藏此格」一并显示；左键点击图标仍触发 {@code onActivate}。</p>
 * <p><b>接入示例</b>（仅在客户端线程调用）：</p>
 * <pre>{@code
 * QuickActionRegistry reg = QuickActionRegistry.get();
 * reg.registerIcon("home", new ItemStack(Items.COMPASS), new Component("回家"), ctx -> { },
 *     new QuickActionContextMenuItem(new Component("子命令"), subCtx -> { }));
 * reg.menuAnchorEntryId("home");
 * }</pre>
 */
@OnlyIn(Dist.CLIENT)
public final class QuickActionRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final QuickActionRegistry INSTANCE = new QuickActionRegistry();

    private final Map<String, QuickActionEntry> entries = new ConcurrentHashMap<>();
    private final List<String> registrationOrder = new ArrayList<>();

    @Nullable
    private volatile String menuAnchorEntryId;

    private QuickActionRegistry() {
    }

    public static QuickActionRegistry get() {
        return INSTANCE;
    }

    /**
     * 设置右键弹出下拉的锚点条目 id（须为已注册且展示方式为 {@link EnumQuickActionDisplay#ICON} 的条目，否则无法作为可见锚点）。
     */
    public void menuAnchorEntryId(@Nullable String id) {
        this.menuAnchorEntryId = id;
    }

    @Nullable
    public String menuAnchorEntryId() {
        return menuAnchorEntryId;
    }

    // region registerIcon 重载

    public void registerIcon(
            @Nonnull String id,
            @Nonnull QuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        register(id, icon, label, EnumQuickActionDisplay.ICON, action, emptyList());
    }

    /**
     * 注册托盘图标，并可选注册该项在右键菜单中的额外行（与「隐藏此格」并列）。
     */
    public void registerIcon(
            @Nonnull String id,
            @Nonnull QuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        register(id, icon, label, EnumQuickActionDisplay.ICON, action, Arrays.asList(contextMenuItems));
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerIcon(id, QuickIcon.item(stack), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerIcon(id, QuickIcon.item(item), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerIcon(id, QuickIcon.effect(effect), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerIcon(id, QuickIcon.resource(texture), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerIcon(id, QuickIcon.item(stack), label, action, contextMenuItems);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerIcon(id, QuickIcon.item(item), label, action, contextMenuItems);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerIcon(id, QuickIcon.effect(effect), label, action, contextMenuItems);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerIcon(id, QuickIcon.resource(texture), label, action, contextMenuItems);
    }

    // endregion

    // region registerListOnly 重载

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        register(id, QuickIcon.none(), label, EnumQuickActionDisplay.LIST_ONLY, action, emptyList());
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull QuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        register(id, icon, label, EnumQuickActionDisplay.LIST_ONLY, action, emptyList());
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull QuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        register(id, icon, label, EnumQuickActionDisplay.LIST_ONLY, action, Arrays.asList(contextMenuItems));
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerListOnly(id, QuickIcon.item(stack), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerListOnly(id, QuickIcon.item(item), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerListOnly(id, QuickIcon.effect(effect), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action
    ) {
        registerListOnly(id, QuickIcon.resource(texture), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerListOnly(id, QuickIcon.item(stack), label, action, contextMenuItems);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerListOnly(id, QuickIcon.item(item), label, action, contextMenuItems);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerListOnly(id, QuickIcon.effect(effect), label, action, contextMenuItems);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull QuickActionContextMenuItem... contextMenuItems
    ) {
        registerListOnly(id, QuickIcon.resource(texture), label, action, contextMenuItems);
    }

    // endregion

    public void register(
            @Nonnull String id,
            @Nonnull QuickIcon icon,
            @Nonnull Component label,
            @Nonnull EnumQuickActionDisplay display,
            @Nullable Consumer<QuickActionContext> action,
            @Nonnull List<QuickActionContextMenuItem> contextMenuItems
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(contextMenuItems, "contextMenuItems");
        QuickActionEntry e = new QuickActionEntry()
                .id(id)
                .quickIcon(icon)
                .label(label)
                .display(display != null ? display : EnumQuickActionDisplay.ICON)
                .onActivate(action);
        e.contextMenuItems.clear();
        e.contextMenuItems.addAll(contextMenuItems);
        entries.put(id, e);
        if (!registrationOrder.contains(id)) {
            registrationOrder.add(id);
        }
        QuickActionOverlay.get().onRegistryChanged();
    }

    /**
     * 当前所有展示类型为 {@link EnumQuickActionDisplay#ICON} 的已注册 id（顺序与注册顺序一致）。
     */
    @Nonnull
    public LinkedHashSet<String> registeredIconEntryIds() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (QuickActionEntry e : allEntriesInOrder()) {
            if (e.display() == EnumQuickActionDisplay.ICON) {
                set.add(e.id());
            }
        }
        return set;
    }

    public void unregister(@Nonnull String id) {
        entries.remove(id);
        registrationOrder.remove(id);
        if (id.equals(menuAnchorEntryId)) {
            menuAnchorEntryId = null;
        }
        QuickActionOverlay.get().onRegistryChanged();
    }

    public void clear() {
        entries.clear();
        registrationOrder.clear();
        menuAnchorEntryId = null;
        QuickActionOverlay.get().onRegistryChanged();
    }

    @Nullable
    public QuickActionEntry getEntry(@Nonnull String id) {
        return entries.get(id);
    }

    @Nonnull
    public List<QuickActionEntry> allEntriesInOrder() {
        List<QuickActionEntry> list = new ArrayList<>();
        for (String id : registrationOrder) {
            QuickActionEntry e = entries.get(id);
            if (e != null) {
                list.add(e);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Nonnull
    public List<String> registeredIds() {
        return registrationOrder.stream().filter(entries::containsKey).collect(Collectors.toList());
    }

    /**
     * 下拉列表展示全部已注册项（含仅列表）。
     */
    @Nonnull
    public List<QuickActionEntry> dropdownEntries() {
        return allEntriesInOrder();
    }

    void validateMenuAnchor() {
        String anchor = menuAnchorEntryId;
        if (anchor == null) {
            return;
        }
        QuickActionEntry e = entries.get(anchor);
        if (e == null || e.display() != EnumQuickActionDisplay.ICON) {
            LOGGER.warn("Inventory quick-action menu anchor '{}' is invalid or not an ICON entry; dropdown disabled until set.", anchor);
        }
    }
}
