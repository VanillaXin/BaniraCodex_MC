package xin.vanilla.banira.client.data;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.enums.EnumCoordinateType;
import xin.vanilla.banira.client.enums.EnumSizeType;
import xin.vanilla.banira.common.util.JsonUtils;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class ScreenCoordinate implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    // region Fields

    private ScreenCoordinate parent;

    private double x = 0;
    private double y = 0;
    private EnumCoordinateType xType = EnumCoordinateType.ABSOLUTE;
    private EnumCoordinateType yType = EnumCoordinateType.ABSOLUTE;

    private double width = 0;
    private double height = 0;
    private EnumSizeType wType = EnumSizeType.ABSOLUTE;
    private EnumSizeType hType = EnumSizeType.ABSOLUTE;

    private Texture texture = Texture.empty();

    // endregion Fields


    // region Constructors

    public ScreenCoordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ScreenCoordinate(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // endregion Constructors


    // region Getters

    public int xInt() {
        return (int) x;
    }

    public int yInt() {
        return (int) y;
    }

    public int widthInt() {
        return (int) width;
    }

    public int heightInt() {
        return (int) height;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    // endregion Getters


    // region Modify

    public ScreenCoordinate addX(double x) {
        this.x += x;
        return this;
    }

    public ScreenCoordinate addY(double y) {
        this.y += y;
        return this;
    }

    public ScreenCoordinate addWidth(double width) {
        this.width += width;
        return this;
    }

    public ScreenCoordinate addHeight(double height) {
        this.height += height;
        return this;
    }

    // endregion Modify


    // region Serialization

    public String toJsonString() {
        return toJson().toString();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.parent != null) {
            JsonObject parentJson = this.parent.toJson();
            if (parentJson != null && !parentJson.entrySet().isEmpty()) {
                json.add("parent", parentJson);
            }
        }
        if (this.x != 0.0) {
            json.addProperty("x", this.x);
        }
        if (this.y != 0.0) {
            json.addProperty("y", this.y);
        }
        if (this.xType != null && this.xType != EnumCoordinateType.ABSOLUTE) {
            json.addProperty("xType", this.xType.name());
        }
        if (this.yType != null && this.yType != EnumCoordinateType.ABSOLUTE) {
            json.addProperty("yType", this.yType.name());
        }

        if (this.width != 0.0) {
            json.addProperty("width", this.width);
        }
        if (this.height != 0.0) {
            json.addProperty("height", this.height);
        }
        if (this.wType != null && this.wType != EnumSizeType.ABSOLUTE) {
            json.addProperty("wType", this.wType.name());
        }
        if (this.hType != null && this.hType != EnumSizeType.ABSOLUTE) {
            json.addProperty("hType", this.hType.name());
        }

        if (this.texture != null) {
            JsonObject textureJson = this.texture.toJson();
            if (textureJson != null && !textureJson.entrySet().isEmpty()) {
                json.add("texture", textureJson);
            }
        }

        return json;
    }

    /**
     * 从 JsonString 反序列化
     */
    public static ScreenCoordinate fromJson(String jsonString) {
        return fromJson(JsonUtils.GSON.fromJson(jsonString, JsonObject.class));
    }

    /**
     * 从 Json 反序列化
     */
    public static ScreenCoordinate fromJson(JsonObject json) {
        ScreenCoordinate coordinate = new ScreenCoordinate();
        if (json.has("parent") && json.get("parent").isJsonObject()) {
            coordinate.parent(fromJson(json.getAsJsonObject("parent")));
        }
        coordinate.x(JsonUtils.getDouble(json, "x", 0.0));
        coordinate.y(JsonUtils.getDouble(json, "y", 0.0));
        if (json.has("xType")) {
            try {
                coordinate.xType(EnumCoordinateType.valueOf(json.get("xType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("yType")) {
            try {
                coordinate.yType(EnumCoordinateType.valueOf(json.get("yType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        coordinate.width(JsonUtils.getDouble(json, "width", 0.0));
        coordinate.height(JsonUtils.getDouble(json, "height", 0.0));
        if (json.has("wType")) {
            try {
                coordinate.wType(EnumSizeType.valueOf(json.get("wType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("hType")) {
            try {
                coordinate.hType(EnumSizeType.valueOf(json.get("hType").getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("texture") && json.get("texture").isJsonObject()) {
            coordinate.texture(Texture.fromJson(json.getAsJsonObject("texture")));
        }
        return coordinate;
    }

    // endregion Serialization


    @Override
    public ScreenCoordinate clone() {
        try {
            ScreenCoordinate cloned = (ScreenCoordinate) super.clone();
            cloned.parent = this.parent != null ? this.parent.clone() : null;
            cloned.x = this.x;
            cloned.y = this.y;
            cloned.xType = this.xType;
            cloned.yType = this.yType;
            cloned.width = this.width;
            cloned.height = this.height;
            cloned.wType = this.wType;
            cloned.hType = this.hType;
            cloned.texture = this.texture;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

}
