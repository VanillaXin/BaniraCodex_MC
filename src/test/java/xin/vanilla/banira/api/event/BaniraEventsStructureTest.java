package xin.vanilla.banira.api.event;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 约束 1.20.1 提供同名、可注销的稳定服务端事件入口。 */
public class BaniraEventsStructureTest {
    @Test
    public void stableEventFacadeExistsAndReturnsRegistrations() throws Exception {
        Path facade = Paths.get("src/main/java/xin/vanilla/banira/api/event/BaniraEvents.java");
        Path bus = Paths.get("src/main/java/xin/vanilla/banira/common/util/BaniraEventBus.java");
        assertTrue(Files.isRegularFile(facade));

        String source = new String(Files.readAllBytes(bus), StandardCharsets.UTF_8);
        assertTrue(source.contains("BaniraEventRegistration onTick"));
        assertTrue(source.contains("BaniraEventRegistration onSave"));
        assertTrue(source.contains("createRegistration"));
    }
}
