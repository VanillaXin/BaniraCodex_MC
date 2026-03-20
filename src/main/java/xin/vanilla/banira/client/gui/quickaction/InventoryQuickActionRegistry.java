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

/**
 * 背包界面快捷操作注册器。仅客户端使用。
 * <p>
 * 注册为 {@link EnumInventoryQuickActionDisplay#ICON} 的项显示在图标组；
 * {@link EnumInventoryQuickActionDisplay#LIST_ONLY} 仅出现在「菜单锚点」图标的右键下拉列表中。
 * </p>
 * <p>图标可使用 {@link ItemStack}、{@link Item}、{@link Effect} 或 {@link ResourceLocation}（纹理），见各类 {@code register*} 重载。</p>
 * <p><b>接入示例</b>（仅在客户端线程调用）：</p>
 * <pre>{@code
 * InventoryQuickActionRegistry reg = InventoryQuickActionRegistry.get();
 * reg.registerIcon("home", new ItemStack(Items.COMPASS), new Component("回家"), ctx -> { });
 * reg.registerIcon("buff", Effects.MOVEMENT_SPEED, new Component("速度"), ctx -> { });
 * reg.registerIcon("gem", new ResourceLocation("minecraft", "textures/item/emerald.png"),
 *         new Component("资源图"), ctx -> { });
 * reg.menuAnchorEntryId("home");
 * }</pre>
 */
@OnlyIn(Dist.CLIENT)
public final class InventoryQuickActionRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final InventoryQuickActionRegistry INSTANCE = new InventoryQuickActionRegistry();

    private final Map<String, InventoryQuickActionEntry> entries = new ConcurrentHashMap<>();
    private final List<String> registrationOrder = new ArrayList<>();

    @Nullable
    private volatile String menuAnchorEntryId;

    private InventoryQuickActionRegistry() {
    }

    public static InventoryQuickActionRegistry get() {
        return INSTANCE;
    }

    /**
     * 设置右键弹出下拉的锚点条目 id（须为已注册且展示方式为 {@link EnumInventoryQuickActionDisplay#ICON} 的条目，否则无法作为可见锚点）。
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
            @Nonnull InventoryQuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        register(id, icon, label, EnumInventoryQuickActionDisplay.ICON, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerIcon(id, InventoryQuickIcon.item(stack), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerIcon(id, InventoryQuickIcon.item(item), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerIcon(id, InventoryQuickIcon.effect(effect), label, action);
    }

    public void registerIcon(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerIcon(id, InventoryQuickIcon.resource(texture), label, action);
    }

    // endregion

    // region registerListOnly 重载

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull InventoryQuickIcon icon,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        register(id, icon, label, EnumInventoryQuickActionDisplay.LIST_ONLY, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ItemStack stack,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerListOnly(id, InventoryQuickIcon.item(stack), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Item item,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerListOnly(id, InventoryQuickIcon.item(item), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull Effect effect,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerListOnly(id, InventoryQuickIcon.effect(effect), label, action);
    }

    public void registerListOnly(
            @Nonnull String id,
            @Nonnull ResourceLocation texture,
            @Nonnull Component label,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        registerListOnly(id, InventoryQuickIcon.resource(texture), label, action);
    }

    // endregion

    public void register(
            @Nonnull String id,
            @Nonnull InventoryQuickIcon icon,
            @Nonnull Component label,
            @Nonnull EnumInventoryQuickActionDisplay display,
            @Nullable Consumer<InventoryQuickActionContext> action
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(icon, "icon");
        InventoryQuickActionEntry e = new InventoryQuickActionEntry()
                .id(id)
                .quickIcon(icon)
                .label(label)
                .display(display != null ? display : EnumInventoryQuickActionDisplay.ICON)
                .onActivate(action);
        entries.put(id, e);
        if (!registrationOrder.contains(id)) {
            registrationOrder.add(id);
        }
        InventoryQuickActionOverlay.get().onRegistryChanged();
    }

    /**
     * 当前所有展示类型为 {@link EnumInventoryQuickActionDisplay#ICON} 的已注册 id（顺序与注册顺序一致）。
     */
    @Nonnull
    public LinkedHashSet<String> registeredIconEntryIds() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (InventoryQuickActionEntry e : allEntriesInOrder()) {
            if (e.display() == EnumInventoryQuickActionDisplay.ICON) {
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
        InventoryQuickActionOverlay.get().onRegistryChanged();
    }

    public void clear() {
        entries.clear();
        registrationOrder.clear();
        menuAnchorEntryId = null;
        InventoryQuickActionOverlay.get().onRegistryChanged();
    }

    @Nullable
    public InventoryQuickActionEntry getEntry(@Nonnull String id) {
        return entries.get(id);
    }

    @Nonnull
    public List<InventoryQuickActionEntry> allEntriesInOrder() {
        List<InventoryQuickActionEntry> list = new ArrayList<>();
        for (String id : registrationOrder) {
            InventoryQuickActionEntry e = entries.get(id);
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
    public List<InventoryQuickActionEntry> dropdownEntries() {
        return allEntriesInOrder();
    }

    void validateMenuAnchor() {
        String anchor = menuAnchorEntryId;
        if (anchor == null) {
            return;
        }
        InventoryQuickActionEntry e = entries.get(anchor);
        if (e == null || e.display() != EnumInventoryQuickActionDisplay.ICON) {
            LOGGER.warn("Inventory quick-action menu anchor '{}' is invalid or not an ICON entry; dropdown disabled until set.", anchor);
        }
    }
}
