package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 在玩家背包界面绘制快捷图标组，并处理拖拽、点击与菜单。
 */
@OnlyIn(Dist.CLIENT)
@Accessors(fluent = true)
public final class InventoryQuickActionOverlay {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LAYOUT_FILE = "quick_action.json";
    private static final long LONG_PRESS_MS = 500;
    private static final int MENU_ROW_H = 12;
    private static final int MENU_MIN_W = 100;
    private static final int MENU_MAX_W = 420;
    private static final int MENU_TEXT_PAD_X = 10;
    private static final int MENU_MAX_BODY_H = 200;
    private static final int MENU_SCROLLBAR_GAP = 2;
    private static final int MENU_SCROLLBAR_W = 5;
    /**
     * 菜单行内注册项图标边长（与 {@link #MENU_ROW_H} 配合垂直居中）
     */
    private static final int MENU_ICON_SIZE = 8;
    private static final int MENU_ICON_GAP = 4;
    private static final float CONTEXT_MENU_CORNER_RADIUS = 2;
    private static final float CONTEXT_MENU_BORDER_THICKNESS = 1f;
    /**
     * 在配置的 cellGap 上额外增加格子间距（像素）
     */
    private static final int GRID_GAP_EXTRA = 1;
    /**
     * 图标相对格子内缩，避免贴边
     */
    private static final int ICON_CELL_INSET = 1;
    private static final int CTX_PAGE_ROOT = 0;
    private static final int CTX_PAGE_HIDDEN = 1;
    private static final int CTX_PAGE_LAYOUT = 2;
    private static final int CTX_PAGE_POSITION = 3;
    private static final ResourceLocation BRAND_TEXTURE = new ResourceLocation(BaniraCodex.MODID, "textures/gui/quick_action_brand.png");

    private static final InventoryQuickActionOverlay INSTANCE = new InventoryQuickActionOverlay();

    public static InventoryQuickActionOverlay get() {
        return INSTANCE;
    }

    @Getter
    private final InventoryQuickActionLayout layout = new InventoryQuickActionLayout();

    private boolean layoutLoaded;

    private int lastScreenW;
    private int lastScreenH;

    private int hoveredSlot = -1;

    private boolean contextOpen;
    private int contextX;
    private int contextY;
    private int contextPage;
    private ContextMenuKind contextMenuKind = ContextMenuKind.NONE;
    /**
     * 右键菜单从用户格打开时非空，根页提供「隐藏此格」；系统格为 null
     */
    @Nullable
    private String contextUserEntryIdForHide;
    private int contextScrollPx;
    private boolean contextScrollbarDragging;
    /**
     * 最近一次布局的菜单外框，供点击滚轮与滚动条命中
     */
    private int ctxLayoutX;
    private int ctxLayoutY;
    private int ctxLayoutW;
    private int ctxLayoutH;
    private int ctxInnerTop;
    private int ctxInnerH;
    private int ctxScrollMaxPx;
    private int ctxScrollbarLeft;
    /**
     * 菜单左侧内容区宽度（不含滚动条与外边 +2）
     */
    private int ctxInnerW;
    private boolean ctxNeedsScrollbar;
    private double contextClickMouseX;
    private double contextClickMouseY;

    private boolean leftDownOnPanel;
    private long leftPressStartMs;
    private double leftPressMouseX;
    private double leftPressMouseY;
    private int pressStartedSlot = -1;

    private boolean draggingTray;
    private double dragGrabDx;
    private double dragGrabDy;

    private boolean editIconDragging;
    private int editDragFromSlot = -1;
    private int editDragHoverSlot = -1;

    private volatile boolean savePending;

    @Nullable
    private InventoryQuickIcon cachedSystemIcon;

    @Nullable
    private String contextTooltipLine;

    private InventoryQuickActionOverlay() {
    }

    private enum ContextMenuKind {
        NONE,
        TRAY
    }

    /**
     * 关闭 GUI 或切换界面时复位拖拽/按下状态，避免托盘永久跟随鼠标。
     */
    public void resetInteractionState() {
        leftDownOnPanel = false;
        draggingTray = false;
        editIconDragging = false;
        editDragFromSlot = -1;
        editDragHoverSlot = -1;
        pressStartedSlot = -1;
        contextOpen = false;
        contextMenuKind = ContextMenuKind.NONE;
        contextUserEntryIdForHide = null;
        contextScrollPx = 0;
        contextScrollbarDragging = false;
    }

    public void onRegistryChanged() {
        syncLayoutWithRegistry();
        markSave();
    }

    private void syncLayoutWithRegistry() {
        layout.syncIconBarWithRegistry(InventoryQuickActionRegistry.get().registeredIconEntryIds());
        layout.hiddenIconIds().removeIf(id -> !InventoryQuickActionRegistry.get().registeredIconEntryIds().contains(id));
    }

    private void ensureLoaded() {
        if (layoutLoaded) {
            return;
        }
        layoutLoaded = true;
        Path path = CustomConfig.getConfigDirectory().resolve(LAYOUT_FILE);
        if (!Files.exists(path)) {
            syncLayoutWithRegistry();
            return;
        }
        try {
            String raw = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject o = JsonUtils.parseObject(raw);
            layout.fromJson(o);
            syncLayoutWithRegistry();
        } catch (Exception e) {
            LOGGER.warn("Failed to load inventory quick-action layout: {}", e.getMessage());
            syncLayoutWithRegistry();
        }
    }

    private void markSave() {
        savePending = true;
    }

    public void flushSaveIfNeeded() {
        if (!savePending) {
            return;
        }
        savePending = false;
        new Thread(() -> {
            try {
                Path dir = CustomConfig.getConfigDirectory();
                Files.createDirectories(dir);
                Path path = dir.resolve(LAYOUT_FILE);
                JsonObject o = layout.toJson();
                Files.write(path, JsonUtils.toPrettyString(o).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOGGER.warn("Failed to save inventory quick-action layout: {}", e.getMessage());
            }
        }, "banira-quick-action-save").start();
    }

    public static boolean isSupportedInventoryScreen(@Nullable Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeScreen;
    }

    /**
     * 在菜单项上滚动时消费事件，避免穿透到背包界面。
     */
    public boolean handleMouseScroll(Screen screen, double mouseX, double mouseY, double delta) {
        if (!isSupportedInventoryScreen(screen) || !contextOpen) {
            return false;
        }
        if (mouseX >= ctxLayoutX && mouseX < ctxLayoutX + ctxLayoutW
                && mouseY >= ctxLayoutY && mouseY < ctxLayoutY + ctxLayoutH) {
            int step = (int) Math.round(-delta * MENU_ROW_H * 2);
            contextScrollPx = Math.max(0, Math.min(ctxScrollMaxPx, contextScrollPx + step));
            return true;
        }
        return false;
    }

    private InventoryQuickIcon systemIcon() {
        if (cachedSystemIcon == null) {
            if (TextureUtils.isTextureAvailable(BRAND_TEXTURE)) {
                cachedSystemIcon = InventoryQuickIcon.resource(BRAND_TEXTURE);
            } else {
                cachedSystemIcon = InventoryQuickIcon.item(Items.BOOK);
            }
        }
        return cachedSystemIcon;
    }

    private List<InventoryQuickActionEntry> visibleUserEntries() {
        List<InventoryQuickActionEntry> out = new ArrayList<>();
        InventoryQuickActionRegistry reg = InventoryQuickActionRegistry.get();
        for (String id : layout.iconBarOrder()) {
            if (layout.hiddenIconIds().contains(id)) {
                continue;
            }
            InventoryQuickActionEntry e = reg.getEntry(id);
            if (e != null && e.display() == EnumInventoryQuickActionDisplay.ICON) {
                out.add(e);
            }
        }
        return out;
    }

    private List<String> visibleUserIds() {
        List<String> ids = new ArrayList<>();
        for (InventoryQuickActionEntry e : visibleUserEntries()) {
            ids.add(e.id());
        }
        return ids;
    }

    private List<String> previewUserIdsForDrag(int fromLinearSlot, int toLinearSlot) {
        List<String> vis = new ArrayList<>(visibleUserIds());
        int from = fromLinearSlot - 1;
        int to = toLinearSlot - 1;
        if (from < 0 || from >= vis.size() || to < 0 || to >= vis.size()) {
            return vis;
        }
        String moved = vis.remove(from);
        vis.add(to, moved);
        return vis;
    }

    private int gridRows(int cols, int slots) {
        return Math.max(1, (slots + cols - 1) / cols);
    }

    private int panelWidthPx(int cols, int cell, int gap) {
        return cols * cell + Math.max(0, cols - 1) * gap;
    }

    private int panelHeightPx(int rows, int cell, int gap) {
        return rows * cell + Math.max(0, rows - 1) * gap;
    }

    private int gridGap() {
        return layout.cellGap() + GRID_GAP_EXTRA;
    }

    private void drawSlotBorder(MatrixStack stack, int gx, int gy, int cell, int argbBorder) {
        int t = 1;
        AbstractGui.fill(stack, gx, gy, gx + cell, gy + t, argbBorder);
        AbstractGui.fill(stack, gx, gy + cell - t, gx + cell, gy + cell, argbBorder);
        AbstractGui.fill(stack, gx, gy, gx + t, gy + cell, argbBorder);
        AbstractGui.fill(stack, gx + cell - t, gy, gx + cell, gy + cell, argbBorder);
    }

    /**
     * 编辑模式下悬停用描边高亮，避免半透明底与 3D 物品混合导致物品消失
     */
    private void drawEditModeSlotHoverOutline(MatrixStack stack, int gx, int gy, int cell, int accentRgb) {
        int c = accentRgb | 0xFF000000;
        int inset = 1;
        int x0 = gx + inset;
        int y0 = gy + inset;
        int x1 = gx + cell - inset;
        int y1 = gy + cell - inset;
        int thick = 1;
        AbstractGui.fill(stack, x0, y0, x1, y0 + thick, c);
        AbstractGui.fill(stack, x0, y1 - thick, x1, y1, c);
        AbstractGui.fill(stack, x0, y0, x0 + thick, y1, c);
        AbstractGui.fill(stack, x1 - thick, y0, x1, y1, c);
    }

    private void slotToCr(int slot, int cols, int[] out) {
        out[0] = slot % cols;
        out[1] = slot / cols;
    }

    private void cellOrigin(int trayX, int trayY, int col, int row, int cell, int gap, int[] out) {
        out[0] = trayX + col * (cell + gap);
        out[1] = trayY + row * (cell + gap);
    }

    private int hitSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int maxSlotExclusive) {
        for (int s = 0; s < maxSlotExclusive; s++) {
            int[] cr = new int[2];
            slotToCr(s, cols, cr);
            int[] xy = new int[2];
            cellOrigin(trayX, trayY, cr[0], cr[1], cell, gap, xy);
            if (mx >= xy[0] && my >= xy[1] && mx < xy[0] + cell && my < xy[1] + cell) {
                return s;
            }
        }
        return -1;
    }

    /**
     * 整块托盘矩形（含格子间隙），用于吸收点击、拖动落点辅助。
     */
    private boolean hitPanel(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap) {
        int w = panelWidthPx(cols, cell, gap);
        int h = panelHeightPx(rows, cell, gap);
        return mx >= trayX && my >= trayY && mx < trayX + w && my < trayY + h;
    }

    /**
     * 在间隙等非单元格像素上时，取距鼠标最近的「用户格」线性下标（1..slots-1）。
     */
    private int nearestUserLinearSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slots) {
        int best = 1;
        double bestD = Double.MAX_VALUE;
        for (int s = 1; s < slots; s++) {
            int[] cr = new int[2];
            slotToCr(s, cols, cr);
            int[] xy = new int[2];
            cellOrigin(trayX, trayY, cr[0], cr[1], cell, gap, xy);
            double cx = xy[0] + cell * 0.5;
            double cy = xy[1] + cell * 0.5;
            double d = (mx - cx) * (mx - cx) + (my - cy) * (my - cy);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    /**
     * 解析用户图标拖放目标格：优先精确落在格内，否则在托盘内则取最近用户格。
     */
    private int resolveUserDropLinearSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slots) {
        int h = hitSlot(mx, my, trayX, trayY, cols, rows, cell, gap, slots);
        if (h >= 1) {
            return h;
        }
        if (slots > 1 && hitPanel(mx, my, trayX, trayY, cols, rows, cell, gap)) {
            return nearestUserLinearSlot(mx, my, trayX, trayY, cols, rows, cell, gap, slots);
        }
        return -1;
    }

    private void pollDragCancel() {
        if (!draggingTray) {
            return;
        }
        long win = mc().getWindow().getWindow();
        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!left) {
            draggingTray = false;
            leftDownOnPanel = false;
            pressStartedSlot = -1;
            markSave();
            flushSaveIfNeeded();
        }
    }

    private double scaledCursorX() {
        long win = mc().getWindow().getWindow();
        double[] cx = new double[1];
        double[] cy = new double[1];
        GLFW.glfwGetCursorPos(win, cx, cy);
        int sw = mc().getWindow().getGuiScaledWidth();
        int fw = Math.max(1, mc().getWindow().getWidth());
        return cx[0] * sw / fw;
    }

    private double scaledCursorY() {
        long win = mc().getWindow().getWindow();
        double[] cx = new double[1];
        double[] cy = new double[1];
        GLFW.glfwGetCursorPos(win, cx, cy);
        int sh = mc().getWindow().getGuiScaledHeight();
        int fh = Math.max(1, mc().getWindow().getHeight());
        return cy[0] * sh / fh;
    }

    private void pollEditIconDragEnd() {
        if (!editIconDragging) {
            return;
        }
        long win = mc().getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            finishEditIconDrag(scaledCursorX(), scaledCursorY());
            leftDownOnPanel = false;
            pressStartedSlot = -1;
        }
    }

    private void finishEditIconDrag(double mouseX, double mouseY) {
        if (!editIconDragging) {
            return;
        }
        editIconDragging = false;
        int from = editDragFromSlot;
        editDragFromSlot = -1;
        editDragHoverSlot = -1;

        List<InventoryQuickActionEntry> users = visibleUserEntries();
        int cols = Math.max(1, layout.gridColumns());
        int slots = 1 + users.size();
        int rows = gridRows(cols, slots);
        int cell = layout.cellSize();
        int gap = gridGap();
        int pw = panelWidthPx(cols, cell, gap);
        int ph = panelHeightPx(rows, cell, gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), pw, ph, off);
        double tlX = trayTopLeftX(pw, ph, off[0], off[1]);
        double tlY = trayTopLeftY(pw, ph, off[0], off[1]);
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        int releaseSlot = resolveUserDropLinearSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);
        if (releaseSlot >= 1 && from >= 1 && from != releaseSlot) {
            moveVisibleUserByLinearSlot(from, releaseSlot);
            markSave();
            flushSaveIfNeeded();
        }
    }

    public void render(MatrixStack stack, Screen screen, int mouseX, int mouseY, float partialTicks) {
        if (!isSupportedInventoryScreen(screen)) {
            closeUi();
            return;
        }
        ensureLoaded();
        InventoryQuickActionRegistry.get().validateMenuAnchor();
        pollDragCancel();
        pollEditIconDragEnd();

        contextTooltipLine = null;

        Minecraft mc = Minecraft.getInstance();
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        lastScreenW = mc.getWindow().getGuiScaledWidth();
        lastScreenH = mc.getWindow().getGuiScaledHeight();

        List<InventoryQuickActionEntry> users = visibleUserEntries();
        int cols = Math.max(1, layout.gridColumns());
        int slots = 1 + users.size();
        int rows = gridRows(cols, slots);
        int cell = layout.cellSize();
        int gap = gridGap();
        int pw = panelWidthPx(cols, cell, gap);
        int ph = panelHeightPx(rows, cell, gap);

        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), pw, ph, off);

        double tlX = trayTopLeftX(pw, ph, off[0], off[1]);
        double tlY = trayTopLeftY(pw, ph, off[0], off[1]);

        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
            applyAnchorFromTopLeft(tlX, tlY, pw, ph, off[0], off[1]);
        }

        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);

        hoveredSlot = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);

        stack.pushPose();
        stack.translate(0, 0, 800);

        RenderSystem.enableBlend();

        int borderRgb = theme.border() | 0xFF000000;
        int accentRgb = theme.accentHover();
        int iconSize = Math.max(8, cell - 2 * ICON_CELL_INSET);
        int iconOff = (cell - iconSize) / 2;

        List<String> previewIds = null;
        if (layout.layoutEditMode() && editIconDragging && editDragFromSlot >= 1 && editDragHoverSlot >= 1) {
            previewIds = previewUserIdsForDrag(editDragFromSlot, editDragHoverSlot);
        }

        InventoryQuickActionRegistry reg = InventoryQuickActionRegistry.get();
        for (int s = 0; s < slots; s++) {
            int[] cr = new int[2];
            slotToCr(s, cols, cr);
            int[] xy = new int[2];
            cellOrigin(trayXi, trayYi, cr[0], cr[1], cell, gap, xy);
            if (layout.layoutEditMode()) {
                drawSlotBorder(stack, xy[0], xy[1], cell, borderRgb);
            }
            int ix = xy[0] + iconOff;
            int iy = xy[1] + iconOff;
            if (s == 0) {
                systemIcon().render(stack, mc, ix, iy, iconSize);
            } else {
                boolean skipIcon = layout.layoutEditMode() && editIconDragging && s == editDragHoverSlot;
                if (!skipIcon) {
                    InventoryQuickActionEntry drawEntry;
                    if (previewIds != null) {
                        String id = previewIds.get(s - 1);
                        drawEntry = reg.getEntry(id);
                    } else {
                        drawEntry = users.get(s - 1);
                    }
                    if (drawEntry != null) {
                        drawEntry.quickIcon().render(stack, mc, ix, iy, iconSize);
                    }
                }
            }
            if (layout.layoutEditMode() && s == hoveredSlot) {
                drawEditModeSlotHoverOutline(stack, xy[0], xy[1], cell, accentRgb);
            } else if (s == hoveredSlot && !layout.layoutEditMode()) {
                int hi = (theme.accentHover() & 0xFFFFFF) | 0x44000000;
                AbstractGui.fill(stack, xy[0] - 1, xy[1] - 1, xy[0] + cell + 1, xy[1] + cell + 1, hi);
            }
        }

        if (layout.layoutEditMode() && editIconDragging && editDragFromSlot >= 1) {
            String dragId = visibleUserIds().get(editDragFromSlot - 1);
            InventoryQuickActionEntry dragged = reg.getEntry(dragId);
            if (dragged != null) {
                int gx = mouseX - cell / 2;
                int gy = mouseY - cell / 2;
                RenderSystem.enableBlend();
                drawSlotBorder(stack, gx, gy, cell, borderRgb);
                dragged.quickIcon().render(stack, mc, gx + iconOff, gy + iconOff, iconSize);
            }
        }

        if (contextOpen) {
            renderContextMenu(stack, screen, mc, mouseX, mouseY, theme);
        }

        RenderSystem.disableBlend();
        stack.popPose();

        if (contextTooltipLine != null && !contextTooltipLine.isEmpty()) {
            screen.renderTooltip(stack, new StringTextComponent(contextTooltipLine), mouseX, mouseY);
        }
    }

    public void tickInteraction(Screen screen, int mouseX, int mouseY) {
        if (!isSupportedInventoryScreen(screen)) {
            return;
        }
        ensureLoaded();
        pollDragCancel();
        pollEditIconDragEnd();
        updateContextScrollbarDrag(mouseX, mouseY);

        List<InventoryQuickActionEntry> users = visibleUserEntries();
        int cols = Math.max(1, layout.gridColumns());
        int slots = 1 + users.size();
        int rows = gridRows(cols, slots);
        int cell = layout.cellSize();
        int gap = gridGap();
        int pw = panelWidthPx(cols, cell, gap);
        int ph = panelHeightPx(rows, cell, gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), pw, ph, off);
        double tlX = trayTopLeftX(pw, ph, off[0], off[1]);
        double tlY = trayTopLeftY(pw, ph, off[0], off[1]);
        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
        }
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);

        int hit = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);
        boolean inPanel = hitPanel(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap);
        long win = mc().getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (layout.layoutEditMode() && editIconDragging && slots > 1 && inPanel) {
            int h = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);
            editDragHoverSlot = h >= 1 ? h : nearestUserLinearSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);
        }

        if (leftDown && leftDownOnPanel && !draggingTray && !editIconDragging && pressStartedSlot == 0 && inPanel) {
            if (System.currentTimeMillis() - leftPressStartMs >= LONG_PRESS_MS) {
                draggingTray = true;
                dragGrabDx = mouseX - tlX;
                dragGrabDy = mouseY - tlY;
                contextOpen = false;
            }
        }

        if (leftDown && leftDownOnPanel && layout.layoutEditMode() && !draggingTray && !editIconDragging
                && pressStartedSlot >= 1 && inPanel) {
            if (System.currentTimeMillis() - leftPressStartMs >= LONG_PRESS_MS) {
                editIconDragging = true;
                editDragFromSlot = pressStartedSlot;
                editDragHoverSlot = pressStartedSlot;
                contextOpen = false;
            }
        }
    }

    private void updateContextScrollbarDrag(double mouseX, double mouseY) {
        if (!contextScrollbarDragging || !contextOpen) {
            return;
        }
        long win = mc().getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            contextScrollbarDragging = false;
            return;
        }
        if (ctxScrollMaxPx <= 0 || ctxInnerH <= 0) {
            return;
        }
        int contentH = ctxScrollMaxPx + ctxInnerH;
        int thumbH = Math.max(10, ctxInnerH * ctxInnerH / Math.max(1, contentH));
        int track = ctxInnerH - thumbH;
        double t = (mouseY - ctxInnerTop - thumbH / 2.0) / Math.max(1, track);
        t = Math.max(0, Math.min(1, t));
        contextScrollPx = (int) Math.round(t * ctxScrollMaxPx);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public boolean handleMouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (!isSupportedInventoryScreen(screen)) {
            return false;
        }
        ensureLoaded();
        List<InventoryQuickActionEntry> users = visibleUserEntries();
        int cols = Math.max(1, layout.gridColumns());
        int slots = 1 + users.size();
        int rows = gridRows(cols, slots);
        int cell = layout.cellSize();
        int gap = gridGap();
        int pw = panelWidthPx(cols, cell, gap);
        int ph = panelHeightPx(rows, cell, gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), pw, ph, off);
        double tlX = trayTopLeftX(pw, ph, off[0], off[1]);
        double tlY = trayTopLeftY(pw, ph, off[0], off[1]);
        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
        }
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        int slot = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);
        boolean inPanel = hitPanel(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap);

        if (contextOpen && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            layoutContextMenu(buildContextRows(), mc());
            boolean inMenu = mouseX >= ctxLayoutX && mouseY >= ctxLayoutY
                    && mouseX < ctxLayoutX + ctxLayoutW && mouseY < ctxLayoutY + ctxLayoutH;
            if (!inMenu) {
                contextOpen = false;
                contextMenuKind = ContextMenuKind.NONE;
            }
            return true;
        }

        if (tryClickContext(mouseX, mouseY, button)) {
            return true;
        }

        if (!inPanel) {
            closeUi();
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftDownOnPanel = true;
            leftPressStartMs = System.currentTimeMillis();
            leftPressMouseX = mouseX;
            leftPressMouseY = mouseY;
            pressStartedSlot = slot;
            contextOpen = false;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (slot < 0) {
                return true;
            }
            String hideTargetId = slot >= 1 ? users.get(slot - 1).id() : null;
            openTrayContextMenu((int) mouseX, (int) mouseY, hideTargetId);
            return true;
        }

        return true;
    }

    public boolean handleMouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        if (!isSupportedInventoryScreen(screen)) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && contextScrollbarDragging) {
            contextScrollbarDragging = false;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !leftDownOnPanel) {
            return false;
        }
        leftDownOnPanel = false;
        List<InventoryQuickActionEntry> users = visibleUserEntries();
        int cols = Math.max(1, layout.gridColumns());
        int slots = 1 + users.size();
        int rows = gridRows(cols, slots);
        int cell = layout.cellSize();
        int gap = gridGap();
        int pw = panelWidthPx(cols, cell, gap);
        int ph = panelHeightPx(rows, cell, gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), pw, ph, off);
        double tlX = trayTopLeftX(pw, ph, off[0], off[1]);
        double tlY = trayTopLeftY(pw, ph, off[0], off[1]);
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        int releaseSlot = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slots);

        if (draggingTray) {
            draggingTray = false;
            pressStartedSlot = -1;
            markSave();
            flushSaveIfNeeded();
            return true;
        }

        if (editIconDragging) {
            finishEditIconDrag(mouseX, mouseY);
            pressStartedSlot = -1;
            return true;
        }

        long dt = System.currentTimeMillis() - leftPressStartMs;
        if (dt < LONG_PRESS_MS && Math.abs(mouseX - leftPressMouseX) < 5 && Math.abs(mouseY - leftPressMouseY) < 5) {
            if (pressStartedSlot >= 1 && releaseSlot == pressStartedSlot && !layout.layoutEditMode()) {
                fireAction(users.get(releaseSlot - 1), mouseX, mouseY);
            }
        }
        pressStartedSlot = -1;
        return true;
    }

    private void moveVisibleUserByLinearSlot(int fromLinearSlot, int toLinearSlot) {
        int fromUser = fromLinearSlot - 1;
        int toUser = toLinearSlot - 1;
        List<String> bar = layout.iconBarOrder();
        List<Integer> visIdx = new ArrayList<>();
        List<String> visIds = new ArrayList<>();
        InventoryQuickActionRegistry r = InventoryQuickActionRegistry.get();
        for (int i = 0; i < bar.size(); i++) {
            String id = bar.get(i);
            if (layout.hiddenIconIds().contains(id)) {
                continue;
            }
            InventoryQuickActionEntry e = r.getEntry(id);
            if (e != null && e.display() == EnumInventoryQuickActionDisplay.ICON) {
                visIdx.add(i);
                visIds.add(id);
            }
        }
        if (fromUser < 0 || toUser < 0 || fromUser >= visIds.size() || toUser >= visIds.size()) {
            return;
        }
        String moved = visIds.remove(fromUser);
        visIds.add(toUser, moved);
        for (int k = 0; k < visIds.size(); k++) {
            bar.set(visIdx.get(k), visIds.get(k));
        }
    }

    private void fireAction(InventoryQuickActionEntry entry, double mx, double my) {
        if (entry.onActivate() == null) {
            return;
        }
        InventoryQuickActionContext ctx = new InventoryQuickActionContext()
                .minecraft(mc())
                .currentScreen(mc().screen)
                .entryId(entry.id())
                .mouseX(mx)
                .mouseY(my);
        try {
            entry.onActivate().accept(ctx);
        } catch (Throwable t) {
            LOGGER.warn("Inventory quick-action callback failed for {}", entry.id(), t);
        }
    }

    private double trayTopLeftX(double pw, double ph, double offX, double offY) {
        double ax = layout.anchorX();
        if (layout.coordinateModeX() == EnumInventoryQuickCoordinateMode.RELATIVE) {
            return ax * lastScreenW - offX;
        }
        return ax - offX;
    }

    private double trayTopLeftY(double pw, double ph, double offX, double offY) {
        double ay = layout.anchorY();
        if (layout.coordinateModeY() == EnumInventoryQuickCoordinateMode.RELATIVE) {
            return ay * lastScreenH - offY;
        }
        return ay - offY;
    }

    private void applyAnchorFromTopLeft(double tlX, double tlY, double pw, double ph, double offX, double offY) {
        double anchorScreenX = tlX + offX;
        double anchorScreenY = tlY + offY;
        if (layout.coordinateModeX() == EnumInventoryQuickCoordinateMode.RELATIVE) {
            layout.anchorX(anchorScreenX / Math.max(1, lastScreenW));
        } else {
            layout.anchorX(anchorScreenX);
        }
        if (layout.coordinateModeY() == EnumInventoryQuickCoordinateMode.RELATIVE) {
            layout.anchorY(anchorScreenY / Math.max(1, lastScreenH));
        } else {
            layout.anchorY(anchorScreenY);
        }
    }

    private void openTrayContextMenu(int mx, int my, @Nullable String userEntryIdForHide) {
        contextMenuKind = ContextMenuKind.TRAY;
        contextUserEntryIdForHide = userEntryIdForHide;
        contextOpen = true;
        contextPage = CTX_PAGE_ROOT;
        contextScrollPx = 0;
        contextX = mx;
        contextY = my;
    }

    private static String trWord(String key) {
        return Translator.of(BaniraCodex.MODID).translate(EnumI18nType.WORD, key);
    }

    private static String trFormat(String key, Object... args) {
        return String.format(Translator.of(BaniraCodex.MODID).translate(EnumI18nType.FORMAT, key), args);
    }

    private static final class CtxRow {
        final String text;
        final boolean keepOpen;
        final Runnable action;
        @Nullable
        final InventoryQuickIcon menuIcon;

        CtxRow(String text, boolean keepOpen, Runnable action) {
            this(text, keepOpen, action, null);
        }

        CtxRow(String text, boolean keepOpen, Runnable action, @Nullable InventoryQuickIcon menuIcon) {
            this.text = text;
            this.keepOpen = keepOpen;
            this.action = action;
            this.menuIcon = menuIcon;
        }
    }

    private List<CtxRow> buildContextRows() {
        List<CtxRow> L = new ArrayList<>();
        if (contextMenuKind != ContextMenuKind.TRAY) {
            return L;
        }

        if (contextPage == CTX_PAGE_HIDDEN) {
            L.add(new CtxRow(trWord("quick_action.back"), true, () -> contextPage = CTX_PAGE_ROOT));
            if (layout.hiddenIconIds().isEmpty()) {
                L.add(new CtxRow(trWord("quick_action.hidden_empty"), true, () -> {
                }));
            } else {
                for (String id : layout.hiddenIconIds()) {
                    InventoryQuickActionEntry ent = InventoryQuickActionRegistry.get().getEntry(id);
                    L.add(new CtxRow(trFormat("quick_action.unhide", id), true, () -> {
                        layout.hiddenIconIds().remove(id);
                        markSave();
                    }, ent != null ? ent.quickIcon() : null));
                }
            }
            return L;
        }
        if (contextPage == CTX_PAGE_LAYOUT) {
            L.add(new CtxRow(trWord("quick_action.back"), true, () -> contextPage = CTX_PAGE_ROOT));
            L.add(new CtxRow(trFormat("quick_action.cell_minus", layout.cellSize()), true,
                    () -> layout.cellSize(Math.max(8, layout.cellSize() - 1))));
            L.add(new CtxRow(trFormat("quick_action.cell_plus", layout.cellSize()), true,
                    () -> layout.cellSize(Math.min(48, layout.cellSize() + 1))));
            L.add(new CtxRow(trFormat("quick_action.cols_minus", layout.gridColumns()), true,
                    () -> layout.gridColumns(Math.max(1, layout.gridColumns() - 1))));
            L.add(new CtxRow(trFormat("quick_action.cols_plus", layout.gridColumns()), true,
                    () -> layout.gridColumns(Math.min(16, layout.gridColumns() + 1))));
            return L;
        }
        if (contextPage == CTX_PAGE_POSITION) {
            L.add(new CtxRow(trWord("quick_action.back"), true, () -> contextPage = CTX_PAGE_ROOT));
            L.add(new CtxRow(trWord("quick_action.reset_anchor"), true, this::resetAnchorPreset));
            String xMode = layout.coordinateModeX() == EnumInventoryQuickCoordinateMode.RELATIVE
                    ? trWord("quick_action.coord_rel") : trWord("quick_action.coord_abs");
            L.add(new CtxRow(trFormat("quick_action.pos_axis", "X", layout.anchorX(), xMode), true, () ->
                    layout.coordinateModeX(layout.coordinateModeX() == EnumInventoryQuickCoordinateMode.RELATIVE
                            ? EnumInventoryQuickCoordinateMode.ABSOLUTE
                            : EnumInventoryQuickCoordinateMode.RELATIVE)));
            String yMode = layout.coordinateModeY() == EnumInventoryQuickCoordinateMode.RELATIVE
                    ? trWord("quick_action.coord_rel") : trWord("quick_action.coord_abs");
            L.add(new CtxRow(trFormat("quick_action.pos_axis", "Y", layout.anchorY(), yMode), true, () ->
                    layout.coordinateModeY(layout.coordinateModeY() == EnumInventoryQuickCoordinateMode.RELATIVE
                            ? EnumInventoryQuickCoordinateMode.ABSOLUTE
                            : EnumInventoryQuickCoordinateMode.RELATIVE)));
            L.add(new CtxRow(trFormat("quick_action.anchor_cycle", layout.groupAnchor().name()), true, () -> {
                EnumPosition[] vals = EnumPosition.values();
                int ni = (layout.groupAnchor().ordinal() + 1) % vals.length;
                layout.groupAnchor(vals[ni]);
            }));
            L.add(new CtxRow(trWord("quick_action.nudge_w"), true, () -> nudgeAnchor(-1, 0)));
            L.add(new CtxRow(trWord("quick_action.nudge_e"), true, () -> nudgeAnchor(1, 0)));
            L.add(new CtxRow(trWord("quick_action.nudge_n"), true, () -> nudgeAnchor(0, -1)));
            L.add(new CtxRow(trWord("quick_action.nudge_s"), true, () -> nudgeAnchor(0, 1)));
            return L;
        }

        // region 根页：任意格统一首项为进入/退出编辑；编辑态下与系统格相同的子菜单
        if (layout.layoutEditMode()) {
            L.add(new CtxRow(trWord("quick_action.exit_edit"), true, () -> layout.layoutEditMode(false)));
            L.add(new CtxRow(trWord("quick_action.menu_layout"), true, () -> contextPage = CTX_PAGE_LAYOUT));
            L.add(new CtxRow(trWord("quick_action.menu_position"), true, () -> contextPage = CTX_PAGE_POSITION));
            L.add(new CtxRow(trWord("quick_action.menu_hidden"), true, () -> contextPage = CTX_PAGE_HIDDEN));
        } else {
            L.add(new CtxRow(trWord("quick_action.enter_edit"), true, () -> layout.layoutEditMode(true)));
            if (contextUserEntryIdForHide != null) {
                L.add(new CtxRow(trWord("quick_action.hide_slot"), false, () -> {
                    layout.hiddenIconIds().add(contextUserEntryIdForHide);
                    markSave();
                    contextOpen = false;
                    contextMenuKind = ContextMenuKind.NONE;
                    contextUserEntryIdForHide = null;
                }));
            }
            // 仅系统格展示注册动作列表
            if (contextUserEntryIdForHide == null) {
                for (InventoryQuickActionEntry e : InventoryQuickActionRegistry.get().allEntriesInOrder()) {
                    if (e.display() != EnumInventoryQuickActionDisplay.ICON) {
                        continue;
                    }
                    L.add(new CtxRow(e.label().toVanilla().getString(), false, () ->
                            fireAction(e, contextClickMouseX, contextClickMouseY), e.quickIcon()));
                }
            }
        }
        // endregion 根页

        return L;
    }

    private void resetAnchorPreset() {
        layout.coordinateModeX(EnumInventoryQuickCoordinateMode.RELATIVE);
        layout.coordinateModeY(EnumInventoryQuickCoordinateMode.RELATIVE);
        layout.anchorX(0.5);
        layout.anchorY(0.02);
        layout.groupAnchor(EnumPosition.TOP_CENTER);
    }

    private String ellipsizeText(FontRenderer font, String s, int maxW) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (font.width(new StringTextComponent(s)) <= maxW) {
            return s;
        }
        String ell = "...";
        if (font.width(new StringTextComponent(ell)) > maxW) {
            return "";
        }
        String t = s;
        while (!t.isEmpty() && font.width(new StringTextComponent(t + ell)) > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        return t + ell;
    }

    private int contextMenuRowTextMaxWidth(int innerW, CtxRow r) {
        if (r.menuIcon != null) {
            return Math.max(0, innerW - MENU_TEXT_PAD_X - MENU_ICON_SIZE - MENU_ICON_GAP - MENU_TEXT_PAD_X);
        }
        return Math.max(0, innerW - MENU_TEXT_PAD_X * 2);
    }

    private void layoutContextMenu(List<CtxRow> rows, Minecraft mc) {
        int n = rows.size();
        int contentH = n * MENU_ROW_H;
        ctxNeedsScrollbar = contentH > MENU_MAX_BODY_H;
        ctxInnerH = Math.min(contentH, MENU_MAX_BODY_H);
        int innerPad = 3;
        int maxTextInner = 0;
        for (CtxRow r : rows) {
            int tw = mc.font.width(new StringTextComponent(r.text));
            int rowW = r.menuIcon != null
                    ? MENU_TEXT_PAD_X + MENU_ICON_SIZE + MENU_ICON_GAP + tw + MENU_TEXT_PAD_X
                    : MENU_TEXT_PAD_X + tw + MENU_TEXT_PAD_X;
            maxTextInner = Math.max(maxTextInner, rowW);
        }
        int sbExtra = ctxNeedsScrollbar ? MENU_SCROLLBAR_GAP + MENU_SCROLLBAR_W : 0;
        int innerW = Math.min(MENU_MAX_W - sbExtra, Math.max(MENU_MIN_W - sbExtra, maxTextInner));
        ctxInnerW = innerW;
        ctxLayoutW = innerW + sbExtra + 2;
        ctxLayoutH = ctxInnerH + innerPad * 2;
        ctxScrollMaxPx = Math.max(0, contentH - ctxInnerH);

        int x = Math.min(contextX, lastScreenW - ctxLayoutW - 4);
        int y = Math.min(contextY, lastScreenH - ctxLayoutH - 4);
        ctxLayoutX = x;
        ctxLayoutY = y;
        ctxInnerTop = y + innerPad;
        ctxScrollbarLeft = x + innerW + MENU_SCROLLBAR_GAP;

        contextScrollPx = Math.max(0, Math.min(ctxScrollMaxPx, contextScrollPx));
    }

    private void renderContextMenu(MatrixStack stack, Screen screen, Minecraft mc, int mouseX, int mouseY, BaniraColorConfig theme) {
        List<CtxRow> rows = buildContextRows();
        layoutContextMenu(rows, mc);

        int x = ctxLayoutX;
        int y = ctxLayoutY;
        int w = ctxLayoutW;
        int h = ctxLayoutH;
        int bg = theme.bgSurface() | 0xFF000000;
        int borderArgb = theme.border() | 0xFF000000;
        ShapeDrawArgs menuFill = ShapeDrawArgs.rect(stack, x, y, w, h, bg);
        menuFill.rect().radius(CONTEXT_MENU_CORNER_RADIUS).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        BaseShapeWidget.drawShape(menuFill);
        ShapeDrawArgs menuOutline = ShapeDrawArgs.rect(stack, x, y, w, h, borderArgb);
        menuOutline.rect().radius(CONTEXT_MENU_CORNER_RADIUS).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE).border(CONTEXT_MENU_BORDER_THICKNESS);
        BaseShapeWidget.drawShape(menuOutline);
        // BaseShapeWidget / AbstractGuiUtils 圆角与描边会 disableBlend；后续滚动条、悬停底与物品图标需混合
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int textColor = theme.textPrimary() | 0xFF000000;
        int innerTop = ctxInnerTop;
        int innerBottom = innerTop + ctxInnerH;

        if (ctxNeedsScrollbar) {
            int sbX = ctxScrollbarLeft;
            AbstractGui.fill(stack, sbX, innerTop, sbX + MENU_SCROLLBAR_W, innerBottom, (theme.border() & 0xFFFFFF) | 0x99000000);
            int contentH = rows.size() * MENU_ROW_H;
            int thumbH = Math.max(10, ctxInnerH * ctxInnerH / Math.max(1, contentH));
            int thumbY = innerTop + (ctxScrollMaxPx <= 0 ? 0 : contextScrollPx * (ctxInnerH - thumbH) / ctxScrollMaxPx);
            int accent = theme.accentHover() | 0xFF000000;
            AbstractGui.fill(stack, sbX + 1, thumbY, sbX + MENU_SCROLLBAR_W - 1, thumbY + thumbH, accent);
        }

        FontRenderer font = mc.font;
        for (int i = 0; i < rows.size(); i++) {
            int ry = innerTop + i * MENU_ROW_H - contextScrollPx;
            int rh = MENU_ROW_H;
            if (ry + rh < innerTop || ry > innerBottom) {
                continue;
            }
            boolean hi = mouseX >= x && mouseX < x + w - (ctxNeedsScrollbar ? MENU_SCROLLBAR_W + MENU_SCROLLBAR_GAP : 0)
                    && mouseY >= ry && mouseY < ry + rh && mouseY >= innerTop && mouseY < innerBottom;
            if (hi) {
                int rowTop = Math.max(ry, innerTop);
                int rowBot = Math.min(ry + rh, innerBottom);
                AbstractGui.fill(stack, x + 2, rowTop, x + w - (ctxNeedsScrollbar ? MENU_SCROLLBAR_W + MENU_SCROLLBAR_GAP + 2 : 2), rowBot,
                        (theme.accentHover() & 0xFFFFFF) | 0x66000000);
            }
            CtxRow row = rows.get(i);
            String full = row.text;
            String shown = ellipsizeText(font, full, contextMenuRowTextMaxWidth(ctxInnerW, row));
            if (row.menuIcon != null) {
                int iconX = x + MENU_TEXT_PAD_X;
                int iconY = ry + (MENU_ROW_H - MENU_ICON_SIZE) / 2;
                row.menuIcon.render(stack, mc, iconX, iconY, MENU_ICON_SIZE);
            }
            float textX = row.menuIcon != null
                    ? x + MENU_TEXT_PAD_X + MENU_ICON_SIZE + MENU_ICON_GAP
                    : x + MENU_TEXT_PAD_X;
            float textY = ry + (MENU_ROW_H - font.lineHeight) / 2f;
            font.draw(stack, new StringTextComponent(shown), textX, textY, textColor);
            if (hi && !shown.equals(full)) {
                contextTooltipLine = full;
            }
        }

        contextX = x;
        contextY = y;
    }

    private boolean tryClickContext(double mouseX, double mouseY, int button) {
        if (!contextOpen || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        contextClickMouseX = mouseX;
        contextClickMouseY = mouseY;

        List<CtxRow> rows = buildContextRows();
        layoutContextMenu(rows, mc());

        int x = ctxLayoutX;
        int y = ctxLayoutY;
        int w = ctxLayoutW;
        int h = ctxLayoutH;
        if (mouseX < x || mouseY < y || mouseX >= x + w || mouseY >= y + h) {
            contextOpen = false;
            contextMenuKind = ContextMenuKind.NONE;
            return true;
        }

        if (ctxNeedsScrollbar) {
            int sbX = ctxScrollbarLeft;
            int innerTop = ctxInnerTop;
            int innerBottom = innerTop + ctxInnerH;
            if (mouseX >= sbX && mouseX < sbX + MENU_SCROLLBAR_W && mouseY >= innerTop && mouseY < innerBottom) {
                contextScrollbarDragging = true;
                updateContextScrollbarDrag(mouseX, mouseY);
                return true;
            }
        }

        int innerTop = ctxInnerTop;
        if (mouseY < innerTop || mouseY >= innerTop + ctxInnerH) {
            return true;
        }
        int relY = (int) mouseY - innerTop + contextScrollPx;
        int idx = relY / MENU_ROW_H;
        if (idx < 0 || idx >= rows.size()) {
            return true;
        }
        CtxRow row = rows.get(idx);
        row.action.run();
        if (!row.keepOpen) {
            contextOpen = false;
            contextMenuKind = ContextMenuKind.NONE;
        }
        markSave();
        flushSaveIfNeeded();
        return true;
    }

    private void nudgeAnchor(int dx, int dy) {
        if (dx != 0) {
            if (layout.coordinateModeX() == EnumInventoryQuickCoordinateMode.RELATIVE) {
                layout.anchorX(layout.anchorX() + dx * 0.015);
            } else {
                layout.anchorX(layout.anchorX() + dx * 4);
            }
        }
        if (dy != 0) {
            if (layout.coordinateModeY() == EnumInventoryQuickCoordinateMode.RELATIVE) {
                layout.anchorY(layout.anchorY() + dy * 0.015);
            } else {
                layout.anchorY(layout.anchorY() + dy * 4);
            }
        }
    }

    private void closeUi() {
        contextOpen = false;
        contextMenuKind = ContextMenuKind.NONE;
    }
}
