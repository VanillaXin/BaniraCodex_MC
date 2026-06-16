package xin.vanilla.banira.internal.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.config.BaniraConfig;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.util.Arrays;
import java.util.List;

/**
 * 测试用配置，包含多层级嵌套与多种类型配置项，用于 ConfigEditorScreen 测试。
 */
@Getter
@Setter
@Accessors(fluent = true)
@Config(name = "banira_codex-test")
public class TestConfig implements ConfigData {

    public enum TestEnum {
        OPTION_A,
        OPTION_B,
        OPTION_C,
        OPTION_D,
        OPTION_E
    }

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "根级配置")
    private RootCategory root = new RootCategory();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "一级分类 A")
    private LevelA levelA = new LevelA();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "一级分类 B")
    private LevelB levelB = new LevelB();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "一级分类 C")
    private LevelC levelC = new LevelC();

    @ConfigEntry.Gui.Tooltip(value = "这才是根级整数")
    @ConfigEntry.BoundedDiscrete(max = 100)
    private int outInt = 42;

    @ConfigEntry.Gui.Tooltip(value = "这才是根级布尔")
    private boolean outBool = true;

    @ConfigEntry.Gui.Tooltip(value = "这才是根级字符串")
    private String outString = "level_out";

    @ConfigEntry.Gui.Tooltip(value = "测试重名布尔")
    private boolean rootBool = false;

    private final ConfigHolder holder;

    private TestConfig() {
        this(null);
    }

    TestConfig(ConfigHolder holder) {
        this.holder = holder;
    }

    public static TestConfig get() {
        return new TestConfig(BaniraConfig.holder(TestConfig.class));
    }

    public ConfigHolder holder() {
        return holder;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class RootCategory {
        @ConfigEntry.Gui.Tooltip(value = "根级字符串")
        private String rootString = "root_value";

        @ConfigEntry.Gui.Tooltip(value = "根级整数")
        @ConfigEntry.BoundedDiscrete(max = 100)
        private int rootInt = 42;

        @ConfigEntry.Gui.Tooltip(value = "根级布尔")
        private boolean rootBool = true;

        @ConfigEntry.Gui.Tooltip(value = "根级长整数")
        @ConfigEntry.BoundedLong(min = 0, max = 10000)
        private long rootLong = 1000L;

        @ConfigEntry.Gui.Tooltip(value = "根级浮点数")
        @ConfigEntry.BoundedDouble(min = 0.0, max = 1.0)
        private double rootDouble = 0.5;

        @ConfigEntry.Gui.Tooltip(value = "根级枚举")
        private TestEnum rootEnum = TestEnum.OPTION_A;

        @ConfigEntry.Gui.Tooltip(value = "根级字符串列表")
        private List<String> rootList = Arrays.asList("a", "b", "c");

        @ConfigEntry.Gui.Tooltip(value = "根级整数列表（元素范围 0–100）")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        private List<Integer> rootIntList = Arrays.asList(1, 2, 3);

        @ConfigEntry.Gui.Tooltip(value = "根级枚举列表")
        private List<TestEnum> rootEnumList = Arrays.asList(TestEnum.OPTION_A, TestEnum.OPTION_C);

        @ConfigEntry.Gui.Tooltip(value = "根级布尔列表")
        private List<Boolean> rootBoolList = Arrays.asList(true, false);
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelA {
        @ConfigEntry.Gui.Tooltip(value = "A 字符串")
        private String aString = "level_a";

        @ConfigEntry.Gui.Tooltip(value = "A 整数")
        @ConfigEntry.BoundedDiscrete(min = -10, max = 10)
        private int aInt = 0;

        @ConfigEntry.Gui.Tooltip(value = "A 布尔")
        private boolean aBool = false;

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(value = "二级分类 A2")
        private LevelA2 a2 = new LevelA2();
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelA2 {
        @ConfigEntry.Gui.Tooltip(value = "A2 字符串")
        private String a2String = "level_a2";

        @ConfigEntry.Gui.Tooltip(value = "A2 浮点数")
        @ConfigEntry.BoundedDouble(min = 0, max = 100)
        private double a2Double = 50.0;

        @ConfigEntry.Gui.Tooltip(value = "A2 枚举")
        private TestEnum a2Enum = TestEnum.OPTION_B;

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(value = "三级分类 A3")
        private LevelA3 a3 = new LevelA3();
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelA3 {
        @ConfigEntry.Gui.Tooltip(value = "A3 字符串")
        private String a3String = "level_a3_deep";

        @ConfigEntry.Gui.Tooltip(value = "A3 字符串列表")
        private List<String> a3List = Arrays.asList("x", "y", "z");

        @ConfigEntry.Gui.Tooltip(value = "A3 整数")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 999)
        private int a3Int = 123;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelB {
        @ConfigEntry.Gui.Tooltip(value = "B 字符串")
        private String bString = "level_b";

        @ConfigEntry.Gui.Tooltip(value = "B 长整数")
        @ConfigEntry.BoundedLong(min = 0, max = 999999)
        private long bLong = 99999L;

        @ConfigEntry.Gui.Tooltip(value = "B 布尔")
        private boolean bBool = true;

        @ConfigEntry.Gui.Tooltip(value = "B 浮点数")
        @ConfigEntry.BoundedDouble(min = -1.0, max = 1.0)
        private double bDouble = 0.0;

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(value = "二级分类 B2")
        private LevelB2 b2 = new LevelB2();
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelB2 {
        @ConfigEntry.Gui.Tooltip(value = "B2 字符串")
        private String b2String = "level_b2";

        @ConfigEntry.Gui.Tooltip(value = "B2 整数")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
        private int b2Int = 25;

        @ConfigEntry.Gui.Tooltip(value = "B2 枚举")
        private TestEnum b2Enum = TestEnum.OPTION_C;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LevelC {
        @ConfigEntry.Gui.Tooltip(value = "C 字符串")
        private String cString = "level_c";

        @ConfigEntry.Gui.Tooltip(value = "C 整数")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        private int cInt = 50;

        @ConfigEntry.Gui.Tooltip(value = "C 布尔")
        private boolean cBool = false;

        @ConfigEntry.Gui.Tooltip(value = "C 枚举")
        private TestEnum cEnum = TestEnum.OPTION_D;

        @ConfigEntry.Gui.Tooltip(value = "C 字符串列表")
        private List<String> cList = Arrays.asList("one", "two", "three");
    }
}
