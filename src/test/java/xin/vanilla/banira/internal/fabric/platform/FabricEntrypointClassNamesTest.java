package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FabricEntrypointClassNamesTest {
    @Test
    public void readsClassEntrypointsAndSkipsMethodEntrypoints() {
        String json = "{\"entrypoints\":{"
                + "\"main\":[\"example.CommonEntry\",\"fabric.ApiHooks::init\"],"
                + "\"client\":[{\"adapter\":\"default\",\"value\":\"example.ClientEntry\"}]}}";

        assertEquals(List.of("example.CommonEntry", "example.ClientEntry"),
                FabricEntrypointClassNames.read(
                        JsonParser.parseString(json).getAsJsonObject(), List.of("main", "client")));
    }

    @Test
    public void ignoresMalformedAndUnselectedEntrypoints() {
        String json = "{\"entrypoints\":{"
                + "\"main\":[{},7,null],"
                + "\"server\":[\"example.ServerEntry\"]}}";

        assertEquals(List.of(), FabricEntrypointClassNames.read(
                JsonParser.parseString(json).getAsJsonObject(), List.of("main")));
    }
}
