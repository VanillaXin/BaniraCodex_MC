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
public class InventoryQuickActionLayout {

    /**
     * 锚点 X 分量：RELATIVE 为屏幕宽度比例，ABSOLUTE 为缩放后像素。
     */
    @Getter
    @Setter
    @Nonnull
    private EnumInventoryQuickCoordinateMode coordinateModeX = EnumInventoryQuickCoordinateMode.RELATIVE;

    /**
     * 锚点 Y 分量：RELATIVE 为屏幕高度比例，ABSOLUTE 为缩放后像素。
     */
    @Getter
    @Setter
    @Nonnull
    private EnumInventoryQuickCoordinateMode coordinateModeY = EnumInventoryQuickCoordinateMode.ABSOLUTE;

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
     * 每行格子数（含首格 Mod 图标），从左向右、自上而下填充。
     */
    @Getter
    @Setter
    private int gridColumns = 6;

    /**
     * 是否处于布局编辑模式（显示格子边框、可拖动排序）。
     */
    @Getter
    @Setter
    private boolean layoutEditMode;

    /**
     * 工具栏上 ICON 类型条目的显示顺序（不含内置 Mod 首格）。
     */
    @Getter
    private final List<String> iconBarOrder = new ArrayList<>();

    @Getter
    private final Set<String> hiddenIconIds = new LinkedHashSet<>();


    public void syncIconBarWithRegistry(@Nonnull Set<String> registeredIconEntryIds) {
        iconBarOrder.removeIf(id -> !registeredIconEntryIds.contains(id));
        for (String id : registeredIconEntryIds) {
            if (!iconBarOrder.contains(id)) {
                iconBarOrder.add(id);
            }
        }
    }

    public JsonObject toJson() {
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
        EnumInventoryQuickCoordinateMode legacy = null;
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
        hiddenIconIds.clear();
        if (o.has("hiddenIconIds") && o.get("hiddenIconIds").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("hiddenIconIds")) {
                if (el.isJsonPrimitive()) {
                    hiddenIconIds.add(el.getAsString());
                }
            }
        }
    }

    private static EnumInventoryQuickCoordinateMode parseMode(String name) {
        if (name == null) {
            return EnumInventoryQuickCoordinateMode.RELATIVE;
        }
        try {
            return EnumInventoryQuickCoordinateMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return EnumInventoryQuickCoordinateMode.RELATIVE;
        }
    }
}
