package xin.vanilla.banira.internal.forge.compat.ipn;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Items;
import net.minecraftforge.fml.ModList;
import org.anti_ad.mc.common.gui.widgets.Widget;
import org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler;
import org.anti_ad.mc.ipnext.gui.inject.EditorWidget;
import org.anti_ad.mc.ipnext.gui.inject.base.ProfileButtonWidget;
import org.anti_ad.mc.ipnext.gui.inject.base.SettingsWidget;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/** Forge 1.16.5 Inventory Profiles Next 设置与布局编辑入口兼容桥。 */
public final class InventoryProfilesNextCompatibility {
    public static final String SOURCE_ID = "inventory_profiles_next";

    private InventoryProfilesNextCompatibility() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("inventoryprofilesnext")) return;
        ExternalInventoryButtonManager.get().registerProvider(new IpnProvider());
    }

    public static boolean shouldSuppressProfileButton(Object value) {
        if (!(value instanceof ProfileButtonWidget)) return false;
        Widget parent = ((ProfileButtonWidget) value).getParent();
        return (parent instanceof SettingsWidget || parent instanceof EditorWidget)
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    private static void openSettings() {
        List<?> widgets = ContainerScreenEventHandler.INSTANCE.getCurrentWidgets();
        if (widgets == null) return;
        for (Object widget : widgets) {
            if (widget instanceof SettingsWidget) {
                ((SettingsWidget) widget).onClick();
                return;
            }
        }
    }

    private static final class IpnProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            return Arrays.asList(
                    new ExternalInventoryAction("settings",
                            BaniraComponent.get().literal(I18n.get(
                                    "inventoryprofiles.tooltip.settings_open")),
                            QuickIcon.item(Items.REPEATER), context -> openSettings()),
                    new ExternalInventoryAction("editor",
                            BaniraComponent.get().literal(I18n.get(
                                    "inventoryprofiles.tooltip.editor_toggle")),
                            QuickIcon.item(Items.WRITABLE_BOOK), context ->
                            ContainerScreenEventHandler.INSTANCE.showEditor())
            );
        }
    }
}
