package xin.vanilla.banira.common.util;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

public final class NativeComponentJson {
    private NativeComponentJson() {
    }

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.encodeStart(
                registries.createSerializationContext(JsonOps.INSTANCE), component).getOrThrow().toString();
    }

    public static MutableComponent fromJson(String json, HolderLookup.Provider registries) {
        return ComponentSerialization.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE),
                JsonUtils.parseElement(json)).getOrThrow().copy();
    }
}
