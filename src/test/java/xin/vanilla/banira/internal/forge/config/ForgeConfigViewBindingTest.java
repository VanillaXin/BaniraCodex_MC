package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.view.ConfigViewBinding;
import xin.vanilla.banira.common.config.view.ConfigViewField;

import java.util.*;

import static org.junit.Assert.*;

public class ForgeConfigViewBindingTest {
    private static class Fields {
        private int count = 3;
        private List<String> rules = Arrays.asList("a,b", "c");
    }

    @Test
    public void realBackendPreservesValidationListsAndExplicitSaveEvents() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        values.put("count", builder.defineInRange("count", 3, 0, 10));
        values.put("rules", builder.defineList("rules", Arrays.asList("a,b", "c"), v -> v instanceof String));
        ForgeConfigSpec spec = builder.build();
        spec.setConfig(CommentedConfig.inMemory());
        ForgeConfigBackend backend = new ForgeConfigBackend(spec, values);
        ConfigHolder holder = ConfigHolder.create("fixture", "sample", ConfigScope.COMMON, backend,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        ConfigViewField<Integer> count = ConfigViewField.scalar("count", Fields.class, "count", Integer.class);
        ConfigViewField<List<String>> rules = ConfigViewField.list("rules", Fields.class, "rules", String.class);
        ConfigViewBinding view = new ConfigViewBinding(Fields.class, () -> holder,
                Config.UnboundAccess.REQUIRE_REGISTERED, count, rules);
        List<Set<String>> saved = new ArrayList<>();
        holder.onSaved(saved::add);
        assertEquals(Arrays.asList("a,b", "c"), view.read(rules));
        assertThrows(IllegalArgumentException.class, () -> view.write(count, 11));
        assertEquals(Integer.valueOf(3), view.read(count));
        view.write(rules, Arrays.asList("x,y", "z"));
        view.read(rules).clear();
        assertEquals(Arrays.asList("x,y", "z"), view.read(rules));
        assertTrue(saved.isEmpty());
        holder.save();
        assertEquals(Collections.singletonList(Collections.singleton("rules")), saved);
        holder.save();
        assertEquals(1, saved.size());
    }
}
