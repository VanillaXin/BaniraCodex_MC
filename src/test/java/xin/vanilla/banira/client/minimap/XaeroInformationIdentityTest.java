package xin.vanilla.banira.client.minimap;

import org.junit.Test;
import xin.vanilla.banira.internal.fabric.compat.minimap.XaeroNotificationInfo;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class XaeroInformationIdentityTest {
    @Test
    public void nativeEnumerationKeepsManagerBoundObjectForEachInformationId() {
        List<Object> first = new ArrayList<>();
        List<Object> second = new ArrayList<>();
        XaeroNotificationInfo.append(first::add);
        XaeroNotificationInfo.append(second::add);
        assertFalse(first.isEmpty());
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertSame("Replacing a same-ID display drops its native manager binding", first.get(i), second.get(i));
        }
    }
}
