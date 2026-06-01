package xin.vanilla.banira.common.data;

import com.google.gson.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.common.util.NumberUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
public class WorldCoordinate implements Serializable, Cloneable {

    // region Fields

    private double x = 0;
    private double y = 0;
    private double z = 0;
    private double yaw = 0;
    private double pitch = 0;
    private double stepSize = 1;
    private RegistryKey<World> dimension = World.OVERWORLD;
    private Direction direction = null;

    // endregion Fields


    // region Constructors

    public WorldCoordinate(@NonNull Entity entity) {
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        this.yaw = entity.yRot;
        this.pitch = entity.xRot;
        this.dimension = entity.level.dimension();
    }

    public WorldCoordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public WorldCoordinate(double x, double y, double z, RegistryKey<World> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public WorldCoordinate(double x, double y, double z, double yaw, double pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public WorldCoordinate(double x, double y, double z, double yaw, double pitch, RegistryKey<World> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }

    // endregion Constructors


    // region Getters

    public int xInt() {
        return (int) x;
    }

    public int yInt() {
        return (int) y;
    }

    public int zInt() {
        return (int) z;
    }

    public int chunkX() {
        return xInt() >> 4;
    }

    public int chunkZ() {
        return zInt() >> 4;
    }


    public String xString() {
        return NumberUtils.toFixedEx(x, 1);
    }

    public String yString() {
        return NumberUtils.toFixedEx(y, 1);
    }

    public String zString() {
        return NumberUtils.toFixedEx(z, 1);
    }

    public String xyzString() {
        return NumberUtils.toFixedEx(x, 1) + ", " + NumberUtils.toFixedEx(y, 1) + ", " + NumberUtils.toFixedEx(z, 1);
    }

    public String chunkXZString() {
        return String.format("%d,%d", this.xInt() >> 4, this.zInt() >> 4);
    }

    public String dimensionId() {
        return dimension.location().toString();
    }

    // endregion Getters


    // region Modify

    public WorldCoordinate addX(double x) {
        this.x += x;
        return this;
    }

    public WorldCoordinate addY(double y) {
        this.y += y;
        return this;
    }

    public WorldCoordinate addZ(double z) {
        this.z += z;
        return this;
    }


    public WorldCoordinate above() {
        return this.addY(stepSize);
    }

    public WorldCoordinate below() {
        return this.addY(-stepSize);
    }

    public WorldCoordinate left() {
        return this.addX(-stepSize);
    }

    public WorldCoordinate right() {
        return this.addX(stepSize);
    }

    public WorldCoordinate front() {
        return this.addZ(stepSize);
    }

    public WorldCoordinate back() {
        return this.addZ(-stepSize);
    }

    // endregion Modify


    // region Distance

    public double distanceFrom(WorldCoordinate coordinate) {
        return Math.sqrt(Math.pow(coordinate.x - x, 2) + Math.pow(coordinate.y - y, 2) + Math.pow(coordinate.z - z, 2));
    }

    public double distanceFrom(double x, double y, double z) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2) + Math.pow(z - this.z, 2));
    }

    public double distanceFrom2D(WorldCoordinate coordinate) {
        return Math.sqrt(Math.pow(coordinate.x - x, 2) + Math.pow(coordinate.z - z, 2));
    }

    /**
     * 判断两个坐标是否在指定范围内
     *
     * @param range 范围
     */
    public boolean equalsInRange(WorldCoordinate coordinate, int range) {
        return Math.abs((int) coordinate.x - (int) x) <= range
                && Math.abs((int) coordinate.y - (int) y) <= range
                && Math.abs((int) coordinate.z - (int) z) <= range
                && coordinate.dimension.equals(dimension);
    }


    /**
     * 根据距离和权重生成随机数
     *
     * @param a 最小值
     * @param b 最大值
     * @param c 中心值
     * @param k 权重系数
     * @return 随机数
     */
    public static int randomWithWeight(int a, int b, int c, double k) {
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;
        // 计算每个值的权重
        for (int i = a; i <= b; i++) {
            double weight = 1.0 / (1 + k * Math.abs(i - c));
            weights.add(weight);
            totalWeight += weight;
        }
        // 生成随机数并选中对应的值
        double rand = new Random().nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (rand <= cumulativeWeight) {
                return a + i;
            }
        }
        return a;
    }

    // endregion Distance


    // region Serialization

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public WorldCoordinate fromBlockPos(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        return this;
    }


    public Vector3d toVector3d() {
        return new Vector3d(x, y, z);
    }

    public WorldCoordinate fromVector3d(Vector3d pos) {
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
        return this;
    }


    /**
     * 序列化到 CompoundTag
     */
    public CompoundNBT toTag() {
        CompoundNBT tag = new CompoundNBT();
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putDouble("yaw", yaw);
        tag.putDouble("pitch", pitch);
        tag.putString("dimension", dimension.location().toString());
        return tag;
    }

    /**
     * 从CompoundTag反序列化
     */
    public static WorldCoordinate fromTag(CompoundNBT tag) {
        WorldCoordinate coordinate = new WorldCoordinate();
        coordinate.x = tag.getDouble("x");
        coordinate.y = tag.getDouble("y");
        coordinate.z = tag.getDouble("z");
        coordinate.yaw = tag.getDouble("yaw");
        coordinate.pitch = tag.getDouble("pitch");
        coordinate.dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(tag.getString("dimension")));
        return coordinate;
    }


    /**
     * 序列化到JsonString
     */
    public String toJsonString() {
        return toJson().toString();
    }

    /**
     * 序列化到Json
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonUtils.set(json, "x", x);
        JsonUtils.set(json, "y", y);
        JsonUtils.set(json, "z", z);
        JsonUtils.set(json, "yaw", yaw);
        JsonUtils.set(json, "pitch", pitch);
        JsonUtils.set(json, "dimension", dimension.location().toString());
        return json;
    }

    /**
     * 从JsonString反序列化
     */
    public static WorldCoordinate fromJson(String jsonString) {
        return fromJson(JsonUtils.parseObject(jsonString));
    }

    /**
     * 从Json反序列化
     */
    public static WorldCoordinate fromJson(JsonObject json) {
        WorldCoordinate coordinate = new WorldCoordinate();
        coordinate.x = JsonUtils.getDouble(json, "x", 0);
        coordinate.y = JsonUtils.getDouble(json, "y", 0);
        coordinate.z = JsonUtils.getDouble(json, "z", 0);
        coordinate.yaw = JsonUtils.getDouble(json, "yaw", 0);
        coordinate.pitch = JsonUtils.getDouble(json, "pitch", 0);
        String dimensionStr = JsonUtils.getString(json, "dimension", World.OVERWORLD.location().toString());
        coordinate.dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(dimensionStr));
        return coordinate;
    }

    /**
     * 从字符串反序列化
     */
    public static WorldCoordinate fromString(String str) {
        WorldCoordinate result = null;
        try {
            String[] split = str.split(",");
            if (split.length == 5) {
                RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(split[0].trim()));
                Direction direction = valuOfDirection(split[4].trim());
                result = new WorldCoordinate(NumberUtils.toDouble(split[1]), NumberUtils.toDouble(split[2]), NumberUtils.toDouble(split[3]), dimension).direction(direction);
            } else if (split.length == 4) {
                if (split[0].contains(":")) {
                    RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(split[0].trim()));
                    result = new WorldCoordinate(NumberUtils.toDouble(split[1]), NumberUtils.toDouble(split[2]), NumberUtils.toDouble(split[3]), dimension);
                } else if (Arrays.stream(Direction.values()).anyMatch(dir -> dir.getName().equals(split[3].trim()))) {
                    Direction direction = valuOfDirection(split[3].trim());
                    result = new WorldCoordinate(NumberUtils.toDouble(split[0]), NumberUtils.toDouble(split[1]), NumberUtils.toDouble(split[2])).direction(direction);
                }
            } else if (split.length == 3) {
                result = new WorldCoordinate(NumberUtils.toDouble(split[0]), NumberUtils.toDouble(split[1]), NumberUtils.toDouble(split[2]));
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static Direction valuOfDirection(String str) {
        try {
            return Arrays.stream(Direction.values())
                    .filter(dir -> dir.getName().equalsIgnoreCase(str) || dir.name().equalsIgnoreCase(str))
                    .findFirst().orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // endregion Serialization


    @Override
    public WorldCoordinate clone() {
        try {
            WorldCoordinate cloned = (WorldCoordinate) super.clone();
            cloned.dimension = this.dimension;
            cloned.x = this.x;
            cloned.y = this.y;
            cloned.z = this.z;
            cloned.yaw = this.yaw;
            cloned.pitch = this.pitch;
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
