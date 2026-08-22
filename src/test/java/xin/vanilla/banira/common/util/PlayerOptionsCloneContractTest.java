package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerOptionsCloneContractTest {
    @Test
    public void deathCloneCopiesEveryClientOptionVanillaLeavesBehind() throws Exception {
        String playerUtils = source("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        for (String field : new String[]{
                "getLanguage", "getChatVisibility", "getChatColors",
                "getMainHand"
        }) {
            assertTrue("Missing clone option: " + field, playerUtils.contains(field));
        }
        assertTrue(playerUtils.contains("banira$setChatVisibility"));
        assertTrue(playerUtils.contains("banira$setChatColors"));
        assertTrue(playerUtils.contains("setMainArm"));
        assertTrue(playerUtils.contains("PlayerOptionsManager.set"));

        String manager = source("src/main/java/xin/vanilla/banira/common/util/PlayerOptionsManager.java");
        assertTrue(manager.contains("不承担通用玩家数据持久化"));
        assertFalse(manager.contains("textFilteringEnabled"));
        assertFalse(manager.contains("allowsListing"));
        assertFalse(manager.contains("viewDistance"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
