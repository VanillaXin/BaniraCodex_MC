package xin.vanilla.banira.common.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PlayerOptionsCloneContractTest {
    @Test
    public void deathCloneCopiesEveryClientOptionVanillaLeavesBehind() throws Exception {
        String playerUtils = source("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        for (String field : new String[]{
                "getLanguage", "getChatVisibility", "getChatColors",
                "getViewDistance", "getMainHand", "isTextFilteringEnabled",
                "getAllowsListing"
        }) {
            assertTrue("Missing clone option: " + field, playerUtils.contains(field));
        }
        assertTrue(playerUtils.contains("banira$setChatVisibility"));
        assertTrue(playerUtils.contains("banira$setChatColors"));
        assertTrue(playerUtils.contains("banira$setRequestedViewDistance"));
        assertTrue(playerUtils.contains("banira$setTextFilteringEnabled"));
        assertTrue(playerUtils.contains("setMainArm"));
        assertTrue(playerUtils.contains("PlayerOptionsManager.set"));

        String manager = source("src/main/java/xin/vanilla/banira/common/util/PlayerOptionsManager.java");
        assertTrue(manager.contains("不承担通用玩家数据持久化"));
        assertTrue(manager.contains("textFilteringEnabled"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
