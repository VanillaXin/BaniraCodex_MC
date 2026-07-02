package xin.vanilla.banira.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaniraIdentifierTest {
    @Test
    public void parsesNamespacedIdentifier() {
        BaniraIdentifier identifier = BaniraIdentifier.parse("banira_codex:config/sync");

        assertEquals("banira_codex", identifier.getNamespace());
        assertEquals("config/sync", identifier.getPath());
        assertEquals("banira_codex:config/sync", identifier.asString());
    }

    @Test
    public void defaultsMissingNamespaceToMinecraft() {
        BaniraIdentifier identifier = BaniraIdentifier.parse("stone");

        assertEquals("minecraft", identifier.getNamespace());
        assertEquals("stone", identifier.getPath());
        assertEquals("minecraft:stone", identifier.asString());
    }
}
