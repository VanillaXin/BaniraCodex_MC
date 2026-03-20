package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图标组布局与用户偏好
 */
@Accessors(chain = true, fluent = true)
public class QuickActionLayout {

    /**
     * 锚点 X 分量：RELATIVE 为屏幕宽度比例，ABSOLUTE 为缩放后像素。
     */
    @Getter
    @Setter
    @Nonnull
    private EnumQuickCoordinateMode coordinateModeX = EnumQuickCoordinateMode.RELATIVE;

    /**
     * 锚点 Y 分量：RELATIVE 为屏幕高度比例，ABSOLUTE 为缩放后像素。
     */
    @Getter
    @Setter
    @Nonnull
    private EnumQuickCoordinateMode coordinateModeY = EnumQuickCoordinateMode.ABSOLUTE;

    @Getter
    @Setter
    @Nonnull
    private EnumPosition groupAnchor = EnumPosition.TOP_CENTER;

    @Getter
    @Setter
    private double anchorX = 0.5;

    @Getter
    @Setter
    private double anchorY = 2;

    @Getter
    @Setter
    private int cellSize = 18;

    @Getter
    @Setter
    private int cellGap = 2;

    /**
     * 方形格网边长（列数 = 行数），含首格 Mod 图标；用户区为 {@code gridColumns² - 1} 格。
     */
    @Getter
    private int gridColumns = 6;

    /**
     * 是否处于布局编辑模式（显示格子边框、可拖动排序）。
     */
    @Getter
    @Setter
    private boolean layoutEditMode;

    /**
     * 与注册表同步时的稳定顺序来源；与 {@link #userSlotGrid} 行主序非空项一致。
     */
    @Getter
    private final List<String> iconBarOrder = new ArrayList<>();

    /**
     * 用户槽位：下标 {@code s - 1} 对应线性格号 {@code s}（1 .. gridColumns²-1），空串表示空位。
     */
    @Getter
    private final List<String> userSlotGrid = new ArrayList<>();

    @Getter
    private final Set<String> hiddenIconIds = new LinkedHashSet<>();

    public QuickActionLayout gridColumns(int v) {
        int c = Math.max(1, Math.min(16, v));
        if (c == this.gridColumns) {
            return this;
        }
        int prev = this.gridColumns;
        this.gridColumns = c;
        migrateUserSlotGridForColumnChange(prev, c);
        rebuildIconBarOrderFromGrid();
        return this;
    }

    public int userSlotCount() {
        int c = gridColumns;
        return Math.max(0, c * c - 1);
    }

    public void syncIconBarWithRegistry(@Nonnull Set<String> registeredIconEntryIds) {
        int target = userSlotCount();
        while (userSlotGrid.size() < target) {
            userSlotGrid.add("");
        }
        while (userSlotGrid.size() > target) {
            userSlotGrid.remove(userSlotGrid.size() - 1);
        }
        for (int i = 0; i < userSlotGrid.size(); i++) {
            String id = userSlotGrid.get(i);
            if (id != null && !id.isEmpty() && !registeredIconEntryIds.contains(id)) {
                userSlotGrid.set(i, "");
            }
        }
        for (String id : registeredIconEntryIds) {
            if (indexOfIdInUserGrid(id) >= 0) {
                continue;
            }
            int empty = firstEmptyUserSlotIndex();
            if (empty >= 0) {
                userSlotGrid.set(empty, id);
            }
        }
        rebuildIconBarOrderFromGrid();
    }

    /**
     * 将用户格 {@code fromLinear}/{@code toLinear}（1..gridColumns²-1）之间互换或移入空位。
     */
    public void moveUserBetweenLinearSlots(int fromLinear, int toLinear) {
        if (fromLinear < 1 || toLinear < 1) {
            return;
        }
        int target = userSlotCount();
        if (fromLinear > target || toLinear > target) {
            return;
        }
        int i = fromLinear - 1;
        int j = toLinear - 1;
        String a = slotGet(userSlotGrid, i);
        String b = slotGet(userSlotGrid, j);
        if (a.isEmpty()) {
            return;
        }
        if (b.isEmpty()) {
            userSlotGrid.set(j, a);
            userSlotGrid.set(i, "");
        } else {
            userSlotGrid.set(i, b);
            userSlotGrid.set(j, a);
        }
        rebuildIconBarOrderFromGrid();
    }

    public JsonObject toJson() {
        rebuildIconBarOrderFromGrid();
        JsonObject o = new JsonObject();
        o.addProperty("coordinateModeX", coordinateModeX.name());
        o.addProperty("coordinateModeY", coordinateModeY.name());
        o.addProperty("groupAnchor", groupAnchor.name());
        o.addProperty("anchorX", anchorX);
        o.addProperty("anchorY", anchorY);
        o.addProperty("cellSize", cellSize);
        o.addProperty("cellGap", cellGap);
        o.addProperty("gridColumns", gridColumns);
        o.addProperty("layoutEditMode", layoutEditMode);
        JsonArray bar = new JsonArray();
        for (String id : iconBarOrder) {
            bar.add(id);
        }
        o.add("iconBarOrder", bar);
        JsonArray grid = new JsonArray();
        for (String id : userSlotGrid) {
            grid.add(id == null ? "" : id);
        }
        o.add("userSlotGrid", grid);
        JsonArray hid = new JsonArray();
        for (String id : hiddenIconIds) {
            hid.add(id);
        }
        o.add("hiddenIconIds", hid);
        return o;
    }

    public void fromJson(JsonObject o) {
        if (o == null) {
            return;
        }
        EnumQuickCoordinateMode legacy = null;
        if (o.has("coordinateMode") && !o.has("coordinateModeX") && !o.has("coordinateModeY")) {
            legacy = parseMode(JsonUtils.getString(o, "coordinateMode", "RELATIVE"));
        }
        if (legacy != null) {
            coordinateModeX = legacy;
            coordinateModeY = legacy;
        } else {
            coordinateModeX = parseMode(JsonUtils.getString(o, "coordinateModeX", "RELATIVE"));
            coordinateModeY = parseMode(JsonUtils.getString(o, "coordinateModeY", "RELATIVE"));
        }
        groupAnchor = EnumPosition.valueOfDefault(JsonUtils.getString(o, "groupAnchor", "TOP_CENTER"));
        anchorX = JsonUtils.getDouble(o, "anchorX", anchorX);
        anchorY = JsonUtils.getDouble(o, "anchorY", anchorY);
        cellSize = Math.max(10, Math.min(48, JsonUtils.getInt(o, "cellSize", cellSize)));
        cellGap = Math.max(0, Math.min(16, JsonUtils.getInt(o, "cellGap", cellGap)));
        gridColumns = Math.max(1, Math.min(16, JsonUtils.getInt(o, "gridColumns", gridColumns)));
        if (o.has("layoutEditMode")) {
            layoutEditMode = JsonUtils.getBoolean(o, "layoutEditMode", false);
        }
        iconBarOrder.clear();
        if (o.has("iconBarOrder") && o.get("iconBarOrder").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("iconBarOrder")) {
                if (el.isJsonPrimitive()) {
                    iconBarOrder.add(el.getAsString());
                }
            }
        }
        userSlotGrid.clear();
        if (o.has("userSlotGrid") && o.get("userSlotGrid").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("userSlotGrid")) {
                if (el.isJsonPrimitive()) {
                    userSlotGrid.add(el.getAsString());
                }
            }
        }
        hiddenIconIds.clear();
        if (o.has("hiddenIconIds") && o.get("hiddenIconIds").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("hiddenIconIds")) {
                if (el.isJsonPrimitive()) {
                    hiddenIconIds.add(el.getAsString());
                }
            }
        }
        finalizeUserSlotGridAfterLoad();
    }

    void rebuildIconBarOrderFromGrid() {
        iconBarOrder.clear();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String id : userSlotGrid) {
            if (id != null && !id.isEmpty()) {
                if (seen.add(id)) {
                    iconBarOrder.add(id);
                }
            }
        }
    }

    private void finalizeUserSlotGridAfterLoad() {
        int target = userSlotCount();
        if (userSlotGrid.isEmpty() && !iconBarOrder.isEmpty()) {
            for (int i = 0; i < target; i++) {
                userSlotGrid.add("");
            }
            int w = 0;
            for (String id : iconBarOrder) {
                if (w >= target) {
                    break;
                }
                if (id != null && !id.isEmpty()) {
                    userSlotGrid.set(w++, id);
                }
            }
        } else {
            while (userSlotGrid.size() < target) {
                userSlotGrid.add("");
            }
            if (userSlotGrid.size() > target) {
                List<String> tailIds = new ArrayList<>();
                for (int i = target; i < userSlotGrid.size(); i++) {
                    String id = slotGet(userSlotGrid, i);
                    if (!id.isEmpty()) {
                        tailIds.add(id);
                    }
                }
                userSlotGrid.subList(target, userSlotGrid.size()).clear();
                for (String id : tailIds) {
                    int e = firstEmptyUserSlotIndex();
                    if (e >= 0) {
                        userSlotGrid.set(e, id);
                    }
                }
            }
        }
        rebuildIconBarOrderFromGrid();
    }

    private void migrateUserSlotGridForColumnChange(int oldCols, int newCols) {
        int oldTarget = Math.max(0, oldCols * oldCols - 1);
        int newTarget = Math.max(0, newCols * newCols - 1);
        List<String> newGrid = new ArrayList<>();
        for (int i = 0; i < newTarget; i++) {
            newGrid.add("");
        }
        List<String> overflow = new ArrayList<>();
        for (int i = 0; i < userSlotGrid.size() && i < oldTarget; i++) {
            String id = slotGet(userSlotGrid, i);
            if (id.isEmpty()) {
                continue;
            }
            int s = i + 1;
            int col = s % oldCols;
            int row = s / oldCols;
            if (col < newCols && row < newCols) {
                int sNew = row * newCols + col;
                int j = sNew - 1;
                if (j >= 0 && j < newTarget) {
                    String cur = newGrid.get(j);
                    if (!cur.isEmpty()) {
                        overflow.add(id);
                    } else {
                        newGrid.set(j, id);
                    }
                } else {
                    overflow.add(id);
                }
            } else {
                overflow.add(id);
            }
        }
        for (String id : overflow) {
            int empty = firstEmptyIndexIn(newGrid);
            if (empty >= 0) {
                newGrid.set(empty, id);
            }
        }
        userSlotGrid.clear();
        userSlotGrid.addAll(newGrid);
    }

    private static int firstEmptyIndexIn(List<String> grid) {
        for (int i = 0; i < grid.size(); i++) {
            if (slotGet(grid, i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int firstEmptyUserSlotIndex() {
        return firstEmptyIndexIn(userSlotGrid);
    }

    private int indexOfIdInUserGrid(String id) {
        for (int i = 0; i < userSlotGrid.size(); i++) {
            if (id.equals(slotGet(userSlotGrid, i))) {
                return i;
            }
        }
        return -1;
    }

    private static String slotGet(List<String> grid, int i) {
        if (i < 0 || i >= grid.size()) {
            return "";
        }
        String s = grid.get(i);
        return s == null ? "" : s;
    }

    private static EnumQuickCoordinateMode parseMode(String name) {
        if (name == null) {
            return EnumQuickCoordinateMode.RELATIVE;
        }
        try {
            return EnumQuickCoordinateMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return EnumQuickCoordinateMode.RELATIVE;
        }
    }
}
