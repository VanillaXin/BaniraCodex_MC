package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 在玩家背包界面绘制快捷图标组，并处理拖拽、点击与菜单。
 */
@OnlyIn(Dist.CLIENT)
@Accessors(fluent = true)
@SuppressWarnings("resource")
public final class QuickActionOverlay {

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
     * 托盘格子内物品由 {@link xin.vanilla.banira.client.gui.widget.ItemWidget#renderGuiItemScaled} 在局部 pose 上再 translate z=200，
     * 右键菜单与悬停提示若与物品同层则会被挡住，须在菜单/提示绘制前额外抬高 Z（须大于 200）。
     */
    private static final float CONTEXT_MENU_AND_TRAY_TOOLTIP_Z_OVER_ITEMS = 400f;
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
    /**
     * 托盘根菜单中某注册项的「扩展右键菜单」子页
     */
    private static final int CTX_PAGE_ENTRY_CONTEXT = 4;
    private static final String BANIRA_TEXTURE_NAME = "logo.png";
    private static ResourceLocation BANIRA_TEXTURE = null;

    private static final QuickActionOverlay INSTANCE = new QuickActionOverlay();

    public static QuickActionOverlay get() {
        return INSTANCE;
    }

    /**
     * 资源或纹理重载后调用：清除系统格 logo 与 {@link QuickIcon} 缓存，下次绘制时重新走 {@link TextureUtils#loadCustomTexture}。
     */
    public static void resetSystemIconTextureCache() {
        BANIRA_TEXTURE = null;
        INSTANCE.cachedSystemIcon = null;
    }

    @Getter
    private final QuickActionLayout layout = new QuickActionLayout();

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
    /**
     * {@link #CTX_PAGE_ENTRY_CONTEXT} 时：当前子菜单对应的注册项 id
     */
    @Nullable
    private String contextEntrySubmenuId;
    /**
     * 系统格左键长按打开的菜单：非编辑根页不显示「进入编辑模式」行（编辑模式下首项仍为退出编辑，由 {@link #addExitEditRowWhenLayoutEditMode} 负责）
     */
    private boolean contextOmitEditToggleRow;
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
    private QuickIcon cachedSystemIcon;

    @Nullable
    private String contextTooltipLine;

    private QuickActionOverlay() {
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
        contextOmitEditToggleRow = false;
        contextScrollPx = 0;
        contextScrollbarDragging = false;
    }

    public void onRegistryChanged() {
        syncLayoutWithRegistry();
        markSave();
    }

    private void syncLayoutWithRegistry() {
        layout.syncIconBarWithRegistry(QuickActionRegistry.get().registeredIconEntryIds());
        layout.hiddenIconIds().removeIf(id -> !QuickActionRegistry.get().registeredIconEntryIds().contains(id));
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
            String raw = Files.readString(path);
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
                Files.writeString(path, JsonUtils.toPrettyString(o));
            } catch (Exception e) {
                LOGGER.warn("Failed to save inventory quick-action layout: {}", e.getMessage());
            }
        }, "banira-quick-action-save").start();
    }

    public static boolean isSupportedInventoryScreen(@Nullable Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
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

    private QuickIcon systemIcon() {
        if (cachedSystemIcon == null) {
            if (BANIRA_TEXTURE == null) {
                BANIRA_TEXTURE = TextureUtils.loadCustomTexture(Identifier.id(), BANIRA_TEXTURE_NAME);
            }
            if (TextureUtils.isTextureAvailable(BANIRA_TEXTURE)) {
                cachedSystemIcon = QuickIcon.resource(BANIRA_TEXTURE);
            } else {
                cachedSystemIcon = QuickIcon.item(Items.BOOK);
            }
        }
        return cachedSystemIcon;
    }

    @Nullable
    private QuickActionEntry userEntryAtLinearSlot(int linearSlot) {
        if (linearSlot < 1) {
            return null;
        }
        List<String> g = layout.userSlotGrid();
        int idx = linearSlot - 1;
        if (idx < 0 || idx >= g.size()) {
            return null;
        }
        String id = g.get(idx);
        if (id == null || id.isEmpty() || layout.hiddenIconIds().contains(id)) {
            return null;
        }
        QuickActionEntry e = QuickActionRegistry.get().getEntry(id);
        if (e == null || e.display() != EnumQuickActionDisplay.ICON) {
            return null;
        }
        return e;
    }

    private boolean slotShowsVisibleUserIcon(int linearSlot, List<String> userGrid) {
        if (linearSlot < 1) {
            return false;
        }
        int idx = linearSlot - 1;
        if (idx < 0 || idx >= userGrid.size()) {
            return false;
        }
        String id = userGrid.get(idx);
        if (id == null || id.isEmpty() || layout.hiddenIconIds().contains(id)) {
            return false;
        }
        QuickActionEntry e = QuickActionRegistry.get().getEntry(id);
        return e != null && e.display() == EnumQuickActionDisplay.ICON;
    }

    /**
     * 含系统格 (0,0) 与所有可见用户图标的轴对齐包围盒，用于锚点百分比按实际占位尺寸计算。
     */
    private int[] occupiedColRowBounds(int cols, List<String> userGrid) {
        int minC = 0;
        int maxC = 0;
        int minR = 0;
        int maxR = 0;
        int slots = cols * cols;
        for (int s = 1; s < slots; s++) {
            if (!slotShowsVisibleUserIcon(s, userGrid)) {
                continue;
            }
            int col = s % cols;
            int row = s / cols;
            minC = Math.min(minC, col);
            maxC = Math.max(maxC, col);
            minR = Math.min(minR, row);
            maxR = Math.max(maxR, row);
        }
        return new int[]{minC, maxC, minR, maxR};
    }

    private int contentWidthPx(int minCol, int maxCol, int cell, int gap) {
        int n = maxCol - minCol + 1;
        return n * cell + Math.max(0, n - 1) * gap;
    }

    private int contentHeightPx(int minRow, int maxRow, int cell, int gap) {
        int n = maxRow - minRow + 1;
        return n * cell + Math.max(0, n - 1) * gap;
    }

    private List<String> copyUserSlotGrid() {
        List<String> g = new ArrayList<>();
        for (String s : layout.userSlotGrid()) {
            g.add(s == null ? "" : s);
        }
        int target = layout.userSlotCount();
        while (g.size() < target) {
            g.add("");
        }
        return g;
    }

    private void applyUserGridMoveOnCopy(List<String> g, int fromLinear, int toLinear) {
        if (fromLinear < 1 || toLinear < 1) {
            return;
        }
        int i = fromLinear - 1;
        int j = toLinear - 1;
        if (i < 0 || i >= g.size() || j < 0 || j >= g.size()) {
            return;
        }
        String a = g.get(i) == null ? "" : g.get(i);
        String b = g.get(j) == null ? "" : g.get(j);
        if (a.isEmpty()) {
            return;
        }
        if (b.isEmpty()) {
            g.set(j, a);
            g.set(i, "");
        } else {
            g.set(i, b);
            g.set(j, a);
        }
    }

    private List<String> previewUserSlotGridForDrag(int fromLinearSlot, int toLinearSlot) {
        List<String> g = copyUserSlotGrid();
        applyUserGridMoveOnCopy(g, fromLinearSlot, toLinearSlot);
        return g;
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

    /**
     * 每格绘制前恢复 GUI 纹理/混合/颜色状态，避免上一格悬停半透明或 blit 残留导致下一格 PNG 半透明边缘异常。
     */
    private static void prepareQuickActionSlotDrawState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawSlotBorder(PoseStack stack, int gx, int gy, int cell, int argbBorder) {
        int t = 1;
        AbstractGuiUtils.fill(stack, gx, gy, cell, t, argbBorder);
        AbstractGuiUtils.fill(stack, gx, gy + cell - t, cell, t, argbBorder);
        AbstractGuiUtils.fill(stack, gx, gy, t, cell, argbBorder);
        AbstractGuiUtils.fill(stack, gx + cell - t, gy, t, cell, argbBorder);
    }

    /**
     * 编辑模式下悬停用描边高亮，避免半透明底与 3D 物品混合导致物品消失
     */
    private void drawEditModeSlotHoverOutline(PoseStack stack, int gx, int gy, int cell, int accentRgb) {
        int c = accentRgb | 0xFF000000;
        int inset = 1;
        int x0 = gx + inset;
        int y0 = gy + inset;
        int x1 = gx + cell - inset;
        int y1 = gy + cell - inset;
        int thick = 1;
        AbstractGuiUtils.fill(stack, x0, y0, x1 - x0, thick, c);
        AbstractGuiUtils.fill(stack, x0, y1 - thick, x1 - x0, thick, c);
        AbstractGuiUtils.fill(stack, x0, y0, thick, y1 - y0, c);
        AbstractGuiUtils.fill(stack, x1 - thick, y0, thick, y1 - y0, c);
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
     * 命中格：系统格始终可命中；用户格仅当有可见图标时可命中，除非处于编辑拖拽且允许空位落点。
     */
    private int hitSlotInteractive(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slotsTotal,
                                   List<String> userGrid, boolean allowEmptyUserSlotsForDrop) {
        int h = hitSlot(mx, my, trayX, trayY, cols, rows, cell, gap, slotsTotal);
        if (h < 0) {
            return -1;
        }
        if (h == 0) {
            return 0;
        }
        if (slotShowsVisibleUserIcon(h, userGrid)) {
            return h;
        }
        return allowEmptyUserSlotsForDrop ? h : -1;
    }

    /**
     * 鼠标是否落在任意「有内容的格」上（不含空用户格与间隙），用于是否由快捷栏消费点击。
     */
    private boolean hitAnyActiveSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slotsTotal,
                                     List<String> userGrid) {
        for (int s = 0; s < slotsTotal; s++) {
            if (s >= 1 && !slotShowsVisibleUserIcon(s, userGrid)) {
                continue;
            }
            int[] cr = new int[2];
            slotToCr(s, cols, cr);
            int[] xy = new int[2];
            cellOrigin(trayX, trayY, cr[0], cr[1], cell, gap, xy);
            if (mx >= xy[0] && my >= xy[1] && mx < xy[0] + cell && my < xy[1] + cell) {
                return true;
            }
        }
        return false;
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
     * 在间隙等非单元格像素上时，取距鼠标最近的「用户格」线性下标（1 .. cols²-1）。
     */
    private int nearestUserLinearSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slotsTotal) {
        int best = 1;
        double bestD = Double.MAX_VALUE;
        for (int s = 1; s < slotsTotal; s++) {
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
    private int resolveUserDropLinearSlot(double mx, double my, int trayX, int trayY, int cols, int rows, int cell, int gap, int slotsTotal) {
        int h = hitSlot(mx, my, trayX, trayY, cols, rows, cell, gap, slotsTotal);
        if (h >= 1) {
            return h;
        }
        if (slotsTotal > 1 && hitPanel(mx, my, trayX, trayY, cols, rows, cell, gap)) {
            return nearestUserLinearSlot(mx, my, trayX, trayY, cols, rows, cell, gap, slotsTotal);
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

        int cols = Math.max(1, layout.gridColumns());
        int rows = cols;
        int slotsTotal = cols * cols;
        int cell = layout.cellSize();
        int gap = gridGap();
        List<String> userGrid = layout.userSlotGrid();
        int[] cr = occupiedColRowBounds(cols, userGrid);
        int cw = contentWidthPx(cr[0], cr[1], cell, gap);
        int ch = contentHeightPx(cr[2], cr[3], cell, gap);
        int insetX = cr[0] * (cell + gap);
        int insetY = cr[2] * (cell + gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), cw, ch, off);
        double tlX = trayTopLeftX(off[0], insetX);
        double tlY = trayTopLeftY(off[1], insetY);
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        int releaseSlot = resolveUserDropLinearSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal);
        if (releaseSlot >= 1 && from >= 1 && from != releaseSlot) {
            layout.moveUserBetweenLinearSlots(from, releaseSlot);
            markSave();
            flushSaveIfNeeded();
        }
    }

    public void render(GuiGraphics graphics, Screen screen, int mouseX, int mouseY, float partialTicks) {
        PoseStack stack = graphics.pose();
        if (!isSupportedInventoryScreen(screen)) {
            closeUi();
            return;
        }
        ensureLoaded();
        QuickActionRegistry.get().validateMenuAnchor();
        pollDragCancel();
        pollEditIconDragEnd();

        contextTooltipLine = null;

        Minecraft mc = Minecraft.getInstance();
        BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
        lastScreenW = mc.getWindow().getGuiScaledWidth();
        lastScreenH = mc.getWindow().getGuiScaledHeight();

        int cols = Math.max(1, layout.gridColumns());
        int rows = cols;
        int slotsTotal = cols * cols;
        int cell = layout.cellSize();
        int gap = gridGap();
        List<String> userGrid = layout.userSlotGrid();
        int[] occCr = occupiedColRowBounds(cols, userGrid);
        int cw = contentWidthPx(occCr[0], occCr[1], cell, gap);
        int ch = contentHeightPx(occCr[2], occCr[3], cell, gap);
        int insetX = occCr[0] * (cell + gap);
        int insetY = occCr[2] * (cell + gap);

        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), cw, ch, off);

        double tlX = trayTopLeftX(off[0], insetX);
        double tlY = trayTopLeftY(off[1], insetY);

        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
            applyAnchorFromTopLeft(tlX, tlY, off[0], off[1], insetX, insetY);
        }

        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);

        boolean allowEmptyHover = layout.layoutEditMode() && editIconDragging;
        hoveredSlot = hitSlotInteractive(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal, userGrid, allowEmptyHover);

        stack.pushPose();
        stack.translate(0, 0, 4000);

        RenderSystem.enableBlend();

        int borderRgb = theme.border() | 0xFF000000;
        int accentRgb = theme.accentHover();
        int iconSize = Math.max(8, cell - 2 * ICON_CELL_INSET);
        int iconOff = (cell - iconSize) / 2;

        List<String> previewGrid = null;
        if (layout.layoutEditMode() && editIconDragging && editDragFromSlot >= 1 && editDragHoverSlot >= 1) {
            previewGrid = previewUserSlotGridForDrag(editDragFromSlot, editDragHoverSlot);
        }

        QuickActionRegistry reg = QuickActionRegistry.get();
        for (int s = 0; s < slotsTotal; s++) {
            prepareQuickActionSlotDrawState();
            int[] cr = new int[2];
            slotToCr(s, cols, cr);
            int[] xy = new int[2];
            cellOrigin(trayXi, trayYi, cr[0], cr[1], cell, gap, xy);
            List<String> gChrome = previewGrid != null ? previewGrid : userGrid;
            boolean userFilledChrome = s >= 1 && slotShowsVisibleUserIcon(s, gChrome);
            boolean dropEmptyHint = layout.layoutEditMode() && editIconDragging && s == editDragHoverSlot && s >= 1 && !userFilledChrome;
            if (layout.layoutEditMode() && (s == 0 || userFilledChrome || dropEmptyHint)) {
                drawSlotBorder(stack, xy[0], xy[1], cell, borderRgb);
            }
            int ix = xy[0] + iconOff;
            int iy = xy[1] + iconOff;
            if (s == 0) {
                systemIcon().render(graphics, mc, ix, iy, iconSize);
            } else {
                boolean skipIcon = layout.layoutEditMode() && editIconDragging && s == editDragHoverSlot;
                if (!skipIcon) {
                    List<String> g = previewGrid != null ? previewGrid : userGrid;
                    int gi = s - 1;
                    String id = gi >= 0 && gi < g.size() && g.get(gi) != null ? g.get(gi) : "";
                    QuickActionEntry drawEntry = null;
                    if (!id.isEmpty() && !layout.hiddenIconIds().contains(id)) {
                        drawEntry = reg.getEntry(id);
                    }
                    if (drawEntry != null && drawEntry.display() == EnumQuickActionDisplay.ICON) {
                        drawEntry.quickIcon().render(graphics, mc, ix, iy, iconSize);
                    }
                }
            }
            if (layout.layoutEditMode() && s == hoveredSlot && (s == 0 || userFilledChrome || dropEmptyHint)) {
                drawEditModeSlotHoverOutline(stack, xy[0], xy[1], cell, accentRgb);
            } else if (s == hoveredSlot && !layout.layoutEditMode() && (s == 0 || userFilledChrome)) {
                int hi = (theme.accentHover() & 0xFFFFFF) | 0x44000000;
                AbstractGuiUtils.fill(stack, xy[0] - 1, xy[1] - 1, cell + 2, cell + 2, hi);
            }
        }

        if (layout.layoutEditMode() && editIconDragging && editDragFromSlot >= 1) {
            int di = editDragFromSlot - 1;
            String dragId = di >= 0 && di < userGrid.size() && userGrid.get(di) != null ? userGrid.get(di) : "";
            QuickActionEntry dragged = dragId.isEmpty() ? null : reg.getEntry(dragId);
            if (dragged != null) {
                int gx = mouseX - cell / 2;
                int gy = mouseY - cell / 2;
                prepareQuickActionSlotDrawState();
                drawSlotBorder(stack, gx, gy, cell, borderRgb);
                dragged.quickIcon().render(graphics, mc, gx + iconOff, gy + iconOff, iconSize);
            }
        }

        stack.pushPose();
        stack.translate(0, 0, CONTEXT_MENU_AND_TRAY_TOOLTIP_Z_OVER_ITEMS);

        if (contextOpen) {
            renderContextMenu(graphics, stack, screen, mc, mouseX, mouseY, theme);
        }

        if (contextTooltipLine != null && !contextTooltipLine.isEmpty()) {
            graphics.renderTooltip(mc.font, Component.literal(contextTooltipLine), mouseX, mouseY);
        }

        renderQuickActionEntryIconTooltipIfHovered(graphics, stack, mc, mouseX, mouseY, theme);

        stack.popPose();

        stack.popPose();
    }

    /**
     * 悬停于带 label 的快捷图标时，使用主题 Tooltip 样式绘制说明。
     */
    private void renderQuickActionEntryIconTooltipIfHovered(GuiGraphics graphics, PoseStack stack, Minecraft mc, int mouseX, int mouseY, BaniraColorConfig theme) {
        if (contextOpen) {
            return;
        }
        if (hoveredSlot < 1) {
            return;
        }
        QuickActionEntry ent = userEntryAtLinearSlot(hoveredSlot);
        if (ent == null || ent.label().isEmpty()) {
            return;
        }
        boolean useTexture = theme != null && theme.tooltipUseTexture();
        FontDrawArgs args = FontDrawArgs.ofPopo(Text.from(ent.label()).stack(stack).font(mc.font))
                .x(mouseX)
                .y(mouseY)
                .popupUseTexture(useTexture);
        TooltipWidget.drawPopupMessage(stack, args, theme, null);
    }

    public void tickInteraction(Screen screen, int mouseX, int mouseY) {
        if (!isSupportedInventoryScreen(screen)) {
            return;
        }
        ensureLoaded();
        pollDragCancel();
        pollEditIconDragEnd();
        updateContextScrollbarDrag(mouseX, mouseY);

        int cols = Math.max(1, layout.gridColumns());
        int rows = cols;
        int slotsTotal = cols * cols;
        int cell = layout.cellSize();
        int gap = gridGap();
        List<String> userGrid = layout.userSlotGrid();
        int[] cr = occupiedColRowBounds(cols, userGrid);
        int cw = contentWidthPx(cr[0], cr[1], cell, gap);
        int ch = contentHeightPx(cr[2], cr[3], cell, gap);
        int insetX = cr[0] * (cell + gap);
        int insetY = cr[2] * (cell + gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), cw, ch, off);
        double tlX = trayTopLeftX(off[0], insetX);
        double tlY = trayTopLeftY(off[1], insetY);
        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
        }
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);

        boolean inPanelGrid = hitPanel(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap);
        long win = mc().getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (layout.layoutEditMode() && editIconDragging && slotsTotal > 1) {
            if (inPanelGrid) {
                int h = hitSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal);
                editDragHoverSlot = h >= 1 ? h : nearestUserLinearSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal);
            } else {
                editDragHoverSlot = -1;
            }
        }

        if (leftDown && leftDownOnPanel && !draggingTray && !editIconDragging && pressStartedSlot == 0 && inPanelGrid) {
            if (System.currentTimeMillis() - leftPressStartMs >= LONG_PRESS_MS) {
                draggingTray = true;
                dragGrabDx = mouseX - tlX;
                dragGrabDy = mouseY - tlY;
                contextOpen = false;
            }
        }

        if (leftDown && leftDownOnPanel && layout.layoutEditMode() && !draggingTray && !editIconDragging
                && pressStartedSlot >= 1 && inPanelGrid && userEntryAtLinearSlot(pressStartedSlot) != null) {
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
        int cols = Math.max(1, layout.gridColumns());
        int rows = cols;
        int slotsTotal = cols * cols;
        int cell = layout.cellSize();
        int gap = gridGap();
        List<String> userGrid = layout.userSlotGrid();
        int[] cr = occupiedColRowBounds(cols, userGrid);
        int cw = contentWidthPx(cr[0], cr[1], cell, gap);
        int ch = contentHeightPx(cr[2], cr[3], cell, gap);
        int insetX = cr[0] * (cell + gap);
        int insetY = cr[2] * (cell + gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), cw, ch, off);
        double tlX = trayTopLeftX(off[0], insetX);
        double tlY = trayTopLeftY(off[1], insetY);
        if (draggingTray) {
            tlX = mouseX - dragGrabDx;
            tlY = mouseY - dragGrabDy;
        }
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        boolean inQuickActionTarget = hitAnyActiveSlot(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal, userGrid)
                || (layout.layoutEditMode() && editIconDragging && hitPanel(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap));
        int slot = hitSlotInteractive(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal, userGrid, false);

        if (contextOpen && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (tryContextMenuRowSecondaryOpen(mouseX, mouseY)) {
                return true;
            }
        }

        if (contextOpen && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            layoutContextMenu(buildContextRows(), mc());
            boolean inMenu = mouseX >= ctxLayoutX && mouseY >= ctxLayoutY
                    && mouseX < ctxLayoutX + ctxLayoutW && mouseY < ctxLayoutY + ctxLayoutH;
            if (!inMenu) {
                contextOpen = false;
                contextMenuKind = ContextMenuKind.NONE;
                contextEntrySubmenuId = null;
                contextPage = CTX_PAGE_ROOT;
            }
            return true;
        }

        if (tryClickContext(mouseX, mouseY, button)) {
            return true;
        }

        if (!inQuickActionTarget) {
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
            contextClickMouseX = mouseX;
            contextClickMouseY = mouseY;
            QuickActionEntry userEnt = slot >= 1 ? userEntryAtLinearSlot(slot) : null;
            String hideTargetId = userEnt != null ? userEnt.id() : null;
            openTrayContextMenu((int) mouseX, (int) mouseY, hideTargetId, false);
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
        int cols = Math.max(1, layout.gridColumns());
        int rows = cols;
        int slotsTotal = cols * cols;
        int cell = layout.cellSize();
        int gap = gridGap();
        List<String> userGrid = layout.userSlotGrid();
        int[] cr = occupiedColRowBounds(cols, userGrid);
        int cw = contentWidthPx(cr[0], cr[1], cell, gap);
        int ch = contentHeightPx(cr[2], cr[3], cell, gap);
        int insetX = cr[0] * (cell + gap);
        int insetY = cr[2] * (cell + gap);
        double[] off = new double[2];
        QuickActionAnchorMath.offsetFromTopLeft(layout.groupAnchor(), cw, ch, off);
        double tlX = trayTopLeftX(off[0], insetX);
        double tlY = trayTopLeftY(off[1], insetY);
        int trayXi = (int) Math.round(tlX);
        int trayYi = (int) Math.round(tlY);
        int releaseSlot = hitSlotInteractive(mouseX, mouseY, trayXi, trayYi, cols, rows, cell, gap, slotsTotal, userGrid, false);

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
            if (pressStartedSlot == 0 && releaseSlot == 0) {
                contextClickMouseX = mouseX;
                contextClickMouseY = mouseY;
                openTrayContextMenu((int) mouseX, (int) mouseY, null, true);
            } else if (pressStartedSlot >= 1 && releaseSlot == pressStartedSlot && !layout.layoutEditMode()) {
                QuickActionEntry e = userEntryAtLinearSlot(releaseSlot);
                if (e != null) {
                    fireAction(e, mouseX, mouseY);
                }
            }
        }
        pressStartedSlot = -1;
        return true;
    }

    private void fireAction(QuickActionEntry entry, double mx, double my) {
        if (entry.onActivate() == null) {
            return;
        }
        QuickActionContext ctx = new QuickActionContext()
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

    private double trayTopLeftX(double contentOffX, int contentInsetX) {
        double ax = layout.anchorX();
        if (layout.coordinateModeX() == EnumQuickCoordinateMode.RELATIVE) {
            return ax * lastScreenW - contentOffX - contentInsetX;
        }
        return ax - contentOffX - contentInsetX;
    }

    private double trayTopLeftY(double contentOffY, int contentInsetY) {
        double ay = layout.anchorY();
        if (layout.coordinateModeY() == EnumQuickCoordinateMode.RELATIVE) {
            return ay * lastScreenH - contentOffY - contentInsetY;
        }
        return ay - contentOffY - contentInsetY;
    }

    private void applyAnchorFromTopLeft(double tlX, double tlY, double contentOffX, double contentOffY, int insetX, int insetY) {
        double anchorScreenX = tlX + insetX + contentOffX;
        double anchorScreenY = tlY + insetY + contentOffY;
        if (layout.coordinateModeX() == EnumQuickCoordinateMode.RELATIVE) {
            layout.anchorX(anchorScreenX / Math.max(1, lastScreenW));
        } else {
            layout.anchorX(anchorScreenX);
        }
        if (layout.coordinateModeY() == EnumQuickCoordinateMode.RELATIVE) {
            layout.anchorY(anchorScreenY / Math.max(1, lastScreenH));
        } else {
            layout.anchorY(anchorScreenY);
        }
    }

    private void openTrayContextMenu(int mx, int my, @Nullable String userEntryIdForHide, boolean omitEditToggleRow) {
        contextMenuKind = ContextMenuKind.TRAY;
        contextUserEntryIdForHide = userEntryIdForHide;
        contextOmitEditToggleRow = omitEditToggleRow;
        contextOpen = true;
        contextPage = CTX_PAGE_ROOT;
        contextEntrySubmenuId = null;
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
        final QuickIcon menuIcon;
        /**
         * 非空时：在托盘根菜单中对该行右键可进入该条目的 {@link QuickActionEntry#contextMenuItems} 子菜单
         */
        @Nullable
        final QuickActionEntry entryForSecondaryMenu;

        CtxRow(String text, boolean keepOpen, Runnable action) {
            this(text, keepOpen, action, null, null);
        }

        CtxRow(String text, boolean keepOpen, Runnable action, @Nullable QuickIcon menuIcon) {
            this(text, keepOpen, action, menuIcon, null);
        }

        CtxRow(String text, boolean keepOpen, Runnable action, @Nullable QuickIcon menuIcon, @Nullable QuickActionEntry entryForSecondaryMenu) {
            this.text = text;
            this.keepOpen = keepOpen;
            this.action = action;
            this.menuIcon = menuIcon;
            this.entryForSecondaryMenu = entryForSecondaryMenu;
        }
    }

    /**
     * 编辑模式下托盘菜单首行：退出编辑（子页与根页统一由调用方决定是否插入）。
     */
    private void addExitEditRowWhenLayoutEditMode(List<CtxRow> L) {
        if (!layout.layoutEditMode()) {
            return;
        }
        L.add(new CtxRow(trWord("quick_action.exit_edit"), true, () -> layout.layoutEditMode(false)));
    }

    private void addHideSlotRow(List<CtxRow> L) {
        if (contextUserEntryIdForHide == null) {
            return;
        }
        L.add(new CtxRow(trWord("quick_action.hide_slot"), false, () -> {
            layout.hiddenIconIds().add(contextUserEntryIdForHide);
            markSave();
            contextOpen = false;
            contextMenuKind = ContextMenuKind.NONE;
            contextUserEntryIdForHide = null;
        }));
    }

    private void addEntryContextMenuRows(List<CtxRow> L, @Nullable QuickActionEntry ent) {
        if (ent == null) {
            return;
        }
        for (QuickActionContextMenuItem it : ent.contextMenuItems) {
            if (it == null) {
                continue;
            }
            L.add(new CtxRow(it.getLabel().toVanilla().getString(), false, () -> {
                if (it.getOnActivate() != null) {
                    QuickActionContext ctx = new QuickActionContext()
                            .minecraft(mc())
                            .currentScreen(mc().screen)
                            .entryId(ent.id())
                            .mouseX(contextClickMouseX)
                            .mouseY(contextClickMouseY);
                    it.getOnActivate().accept(ctx);
                }
            }, it.getMenuIcon()));
        }
    }

    /**
     * 系统格菜单
     */
    private void addSystemTrayDropdownRows(List<CtxRow> L) {
        for (QuickActionEntry ent : QuickActionRegistry.get().dropdownEntries()) {
            if (ent == null) {
                continue;
            }
            boolean hasSecondary = !ent.contextMenuItems.isEmpty();
            L.add(new CtxRow(ent.label().toVanilla().getString(), false, () ->
                    fireAction(ent, contextClickMouseX, contextClickMouseY), ent.quickIcon(),
                    hasSecondary ? ent : null));
        }
    }

    private List<CtxRow> buildContextRows() {
        List<CtxRow> L = new ArrayList<>();
        if (contextMenuKind != ContextMenuKind.TRAY) {
            return L;
        }

        if (contextPage == CTX_PAGE_ENTRY_CONTEXT) {
            if (contextEntrySubmenuId == null) {
                contextPage = CTX_PAGE_ROOT;
            } else {
                addExitEditRowWhenLayoutEditMode(L);
                L.add(new CtxRow(trWord("quick_action.back"), true, () -> {
                    contextPage = CTX_PAGE_ROOT;
                    contextEntrySubmenuId = null;
                }));
                QuickActionEntry ent = QuickActionRegistry.get().getEntry(contextEntrySubmenuId);
                addEntryContextMenuRows(L, ent);
                return L;
            }
        }

        if (contextPage == CTX_PAGE_HIDDEN) {
            addExitEditRowWhenLayoutEditMode(L);
            L.add(new CtxRow(trWord("quick_action.back"), true, () -> contextPage = CTX_PAGE_ROOT));
            if (layout.hiddenIconIds().isEmpty()) {
                L.add(new CtxRow(trWord("quick_action.hidden_empty"), true, () -> {
                }));
            } else {
                for (String id : layout.hiddenIconIds()) {
                    QuickActionEntry ent = QuickActionRegistry.get().getEntry(id);
                    String display = id;
                    if (ent != null && !ent.label().isEmpty()) {
                        display = ent.label().toVanilla().getString();
                    }
                    L.add(new CtxRow(trFormat("quick_action.unhide", display), true, () -> {
                        layout.hiddenIconIds().remove(id);
                        markSave();
                    }, ent != null ? ent.quickIcon() : null));
                }
            }
            return L;
        }
        if (contextPage == CTX_PAGE_LAYOUT) {
            addExitEditRowWhenLayoutEditMode(L);
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
            addExitEditRowWhenLayoutEditMode(L);
            L.add(new CtxRow(trWord("quick_action.back"), true, () -> contextPage = CTX_PAGE_ROOT));
            L.add(new CtxRow(trWord("quick_action.reset_anchor"), true, this::resetAnchorPreset));
            String xMode = layout.coordinateModeX() == EnumQuickCoordinateMode.RELATIVE
                    ? trWord("quick_action.coord_rel") : trWord("quick_action.coord_abs");
            L.add(new CtxRow(trFormat("quick_action.pos_axis", "X", layout.anchorX(), xMode), true, () ->
                    layout.coordinateModeX(layout.coordinateModeX() == EnumQuickCoordinateMode.RELATIVE
                            ? EnumQuickCoordinateMode.ABSOLUTE
                            : EnumQuickCoordinateMode.RELATIVE)));
            String yMode = layout.coordinateModeY() == EnumQuickCoordinateMode.RELATIVE
                    ? trWord("quick_action.coord_rel") : trWord("quick_action.coord_abs");
            L.add(new CtxRow(trFormat("quick_action.pos_axis", "Y", layout.anchorY(), yMode), true, () ->
                    layout.coordinateModeY(layout.coordinateModeY() == EnumQuickCoordinateMode.RELATIVE
                            ? EnumQuickCoordinateMode.ABSOLUTE
                            : EnumQuickCoordinateMode.RELATIVE)));
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

        // region 根页：非编辑时系统格可进入编辑；编辑模式下首项为退出编辑，用户格其后为「隐藏此格」+ 该项注册的右键菜单
        QuickActionEntry hideTargetEntry = contextUserEntryIdForHide != null
                ? QuickActionRegistry.get().getEntry(contextUserEntryIdForHide)
                : null;

        if (layout.layoutEditMode()) {
            addExitEditRowWhenLayoutEditMode(L);
            if (contextUserEntryIdForHide == null) {
                L.add(new CtxRow(trWord("quick_action.menu_layout"), true, () -> contextPage = CTX_PAGE_LAYOUT));
                L.add(new CtxRow(trWord("quick_action.menu_position"), true, () -> contextPage = CTX_PAGE_POSITION));
                L.add(new CtxRow(trWord("quick_action.menu_hidden"), true, () -> contextPage = CTX_PAGE_HIDDEN));
            } else {
                addHideSlotRow(L);
                addEntryContextMenuRows(L, hideTargetEntry);
            }
        } else {
            if (contextUserEntryIdForHide == null) {
                if (!contextOmitEditToggleRow) {
                    L.add(new CtxRow(trWord("quick_action.enter_edit"), true, () -> layout.layoutEditMode(true)));
                }
                addSystemTrayDropdownRows(L);
                if (contextOmitEditToggleRow && L.isEmpty()) {
                    L.add(new CtxRow(trWord("quick_action.no_registered_entries"), true, () -> {
                    }));
                }
            } else {
                addHideSlotRow(L);
                addEntryContextMenuRows(L, hideTargetEntry);
            }
        }
        // endregion 根页

        return L;
    }

    private void resetAnchorPreset() {
        final QuickActionLayout DEFAULT = new QuickActionLayout();
        layout.coordinateModeX(DEFAULT.coordinateModeX());
        layout.coordinateModeY(DEFAULT.coordinateModeY());
        layout.anchorX(DEFAULT.anchorX());
        layout.anchorY(DEFAULT.anchorY());
        layout.groupAnchor(DEFAULT.groupAnchor());
    }

    private String ellipsizeText(Font font, String s, int maxW) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (font.width(s) <= maxW) {
            return s;
        }
        String ell = "...";
        if (font.width(ell) > maxW) {
            return "";
        }
        String t = s;
        while (!t.isEmpty() && font.width(t + ell) > maxW) {
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
        int contentH = Math.max(MENU_ROW_H, n * MENU_ROW_H);
        ctxNeedsScrollbar = contentH > MENU_MAX_BODY_H;
        ctxInnerH = Math.min(contentH, MENU_MAX_BODY_H);
        int innerPad = 3;
        int maxTextInner = 0;
        for (CtxRow r : rows) {
            int tw = mc.font.width(r.text);
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

    private void renderContextMenu(GuiGraphics graphics, PoseStack stack, Screen screen, Minecraft mc, int mouseX, int mouseY, BaniraColorConfig theme) {
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
        // 圆角菜单绘制后需保证混合与 GUI 着色器可用（见 AbstractGuiUtils#restoreGuiRenderState）
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int textColor = theme.textPrimary() | 0xFF000000;
        int innerTop = ctxInnerTop;
        int innerBottom = innerTop + ctxInnerH;

        if (ctxNeedsScrollbar) {
            int sbX = ctxScrollbarLeft;
            AbstractGuiUtils.fill(stack, sbX, innerTop, MENU_SCROLLBAR_W, innerBottom - innerTop, (theme.border() & 0xFFFFFF) | 0x99000000);
            int contentH = rows.size() * MENU_ROW_H;
            int thumbH = Math.max(10, ctxInnerH * ctxInnerH / Math.max(1, contentH));
            int thumbY = innerTop + (ctxScrollMaxPx <= 0 ? 0 : contextScrollPx * (ctxInnerH - thumbH) / ctxScrollMaxPx);
            int accent = theme.accentHover() | 0xFF000000;
            AbstractGuiUtils.fill(stack, sbX + 1, thumbY, MENU_SCROLLBAR_W - 2, thumbH, accent);
        }

        Font font = mc.font;
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
                int rowFillRight = x + w - (ctxNeedsScrollbar ? MENU_SCROLLBAR_W + MENU_SCROLLBAR_GAP + 2 : 2);
                AbstractGuiUtils.fill(stack, x + 2, rowTop, rowFillRight - (x + 2), rowBot - rowTop,
                        (theme.accentHover() & 0xFFFFFF) | 0x66000000);
            }
            CtxRow row = rows.get(i);
            String full = row.text;
            String shown = ellipsizeText(font, full, contextMenuRowTextMaxWidth(ctxInnerW, row));
            if (row.menuIcon != null) {
                int iconX = x + MENU_TEXT_PAD_X;
                int iconY = ry + (MENU_ROW_H - MENU_ICON_SIZE) / 2;
                row.menuIcon.renderForMenu(graphics, mc, iconX, iconY, MENU_ICON_SIZE);
            }
            float textX = row.menuIcon != null
                    ? x + MENU_TEXT_PAD_X + MENU_ICON_SIZE + MENU_ICON_GAP
                    : x + MENU_TEXT_PAD_X;
            float textY = ry + (MENU_ROW_H - font.lineHeight) / 2f;
            graphics.drawString(font, shown, textX, textY, textColor, false);
            if (hi && !shown.equals(full)) {
                contextTooltipLine = full;
            }
        }

        AbstractGuiUtils.restoreGuiRenderState();

        contextX = x;
        contextY = y;
    }

    /**
     * 托盘菜单内：在根列表行上右键进入该条目的 {@link QuickActionEntry#contextMenuItems} 子菜单。
     *
     * @return true 表示已处理（含子菜单已打开、滚动条命中等），不应再交给外层关闭逻辑
     */
    private boolean tryContextMenuRowSecondaryOpen(double mouseX, double mouseY) {
        if (!contextOpen || contextMenuKind != ContextMenuKind.TRAY) {
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
            return false;
        }

        if (ctxNeedsScrollbar) {
            int sbX = ctxScrollbarLeft;
            int innerTop = ctxInnerTop;
            int innerBottom = innerTop + ctxInnerH;
            if (mouseX >= sbX && mouseX < sbX + MENU_SCROLLBAR_W && mouseY >= innerTop && mouseY < innerBottom) {
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
        QuickActionEntry sec = row.entryForSecondaryMenu;
        if (sec == null || sec.contextMenuItems.isEmpty()) {
            return false;
        }
        contextEntrySubmenuId = sec.id();
        contextPage = CTX_PAGE_ENTRY_CONTEXT;
        contextScrollPx = 0;
        return true;
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
            contextEntrySubmenuId = null;
            contextPage = CTX_PAGE_ROOT;
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
            contextEntrySubmenuId = null;
            contextPage = CTX_PAGE_ROOT;
        }
        markSave();
        flushSaveIfNeeded();
        return true;
    }

    private void nudgeAnchor(int dx, int dy) {
        if (dx != 0) {
            if (layout.coordinateModeX() == EnumQuickCoordinateMode.RELATIVE) {
                layout.anchorX(layout.anchorX() + dx * 0.015);
            } else {
                layout.anchorX(layout.anchorX() + dx * 4);
            }
        }
        if (dy != 0) {
            if (layout.coordinateModeY() == EnumQuickCoordinateMode.RELATIVE) {
                layout.anchorY(layout.anchorY() + dy * 0.015);
            } else {
                layout.anchorY(layout.anchorY() + dy * 4);
            }
        }
    }

    private void closeUi() {
        contextOpen = false;
        contextMenuKind = ContextMenuKind.NONE;
        contextOmitEditToggleRow = false;
        contextEntrySubmenuId = null;
        contextPage = CTX_PAGE_ROOT;
    }
}
