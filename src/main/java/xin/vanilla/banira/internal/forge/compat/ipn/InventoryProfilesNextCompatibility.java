package xin.vanilla.banira.internal.forge.compat.ipn;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.anti_ad.mc.common.gui.widgets.Widget;
import org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler;
import org.anti_ad.mc.ipnext.gui.inject.EditorWidget;
import org.anti_ad.mc.ipnext.gui.inject.base.ProfileButtonWidget;
import org.anti_ad.mc.ipnext.gui.inject.base.SettingsWidget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.internal.compat.IpnInventoryContract;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Forge 1.16.5 Inventory Profiles Next 设置与布局编辑入口兼容桥。
 */
public final class InventoryProfilesNextCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String SOURCE_ID = "inventory_profiles_next";
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(
            "inventoryprofilesnext", "textures/gui/gui_buttons.png");
    private static volatile boolean ready;

    private InventoryProfilesNextCompatibility() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("inventoryprofilesnext")) return;
        if (ready) return;
        try {
            if (!IpnInventoryContract.matches()) throw new IllegalStateException(IpnInventoryContract.mismatch());
            ClassLoader loader = InventoryProfilesNextCompatibility.class.getClassLoader();
            Class.forName("org.anti_ad.mc.ipnext.gui.inject.base.SortButtonWidget", false, loader);
            Class.forName("org.anti_ad.mc.ipnext.gui.inject.base.ProfileButtonWidget", false, loader);
            if (!IpnInventoryContract.hooksApplied()) {
                throw new IllegalStateException("IPN native render/click hooks were not fully applied");
            }
            ready = true;
            ExternalInventoryButtonManager.get().registerProvider(new IpnProvider());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ready = false;
            LOGGER.error("IPN compatibility disabled; native controls remain active", exception);
        }
    }

    public static boolean shouldSuppressProfileButton(Object value) {
        if (!ready || !(value instanceof ProfileButtonWidget)) return false;
        try {
            Widget parent = ((ProfileButtonWidget) value).getParent();
            return (parent instanceof SettingsWidget || parent instanceof EditorWidget)
                    && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
        } catch (RuntimeException | LinkageError exception) {
            ready = false;
            LOGGER.error("IPN compatibility disabled; native controls remain active", exception);
            return false;
        }
    }

    private static void openSettings() {
        if (!ready) return;
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
            if (!ready) throw new IllegalStateException("IPN native bridge is not ready");
            return Arrays.asList(
                    new ExternalInventoryAction("settings",
                            BaniraComponent.get().literal(I18n.get(
                                    "inventoryprofiles.tooltip.settings_open")),
                            profileIcon(140, 0), context -> openSettings()),
                    new ExternalInventoryAction("editor",
                            BaniraComponent.get().literal(I18n.get(
                                    "inventoryprofiles.tooltip.editor_toggle")),
                            profileIcon(160, 40), context ->
                            ContainerScreenEventHandler.INSTANCE.showEditor())
            );
        }
    }

    private static QuickIcon profileIcon(int u, int v) {
        Texture texture = Texture.of(BUTTON_TEXTURE, 256, 256)
                .u0(u).v0(v).uWidth(10).vHeight(10);
        return QuickIcon.resource(texture);
    }
}
