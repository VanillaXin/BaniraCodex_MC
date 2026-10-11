package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.api.cost.InventoryScope;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import xin.vanilla.banira.platform.UnavailableCostService;

import static org.junit.Assert.*;

public class BaniraCostApiTest {
    @Test
    public void oldPlatformImplementationsDefaultToExplicitlyUnavailableCosts() {
        assertSame(UnavailableCostService.INSTANCE, new TestBaniraPlatform().costService());
        try {
            new TestBaniraPlatform().costService().access(new Object(), InventoryScope.MAIN);
            fail("Missing platform support became free");
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void costRequirementChildrenAreImmutable() {
        java.util.List<xin.vanilla.banira.api.cost.CostRequirement> children = new java.util.ArrayList<>();
        children.add(xin.vanilla.banira.api.cost.CostRequirement.free());
        xin.vanilla.banira.api.cost.CostRequirement requirement = xin.vanilla.banira.api.cost.CostRequirement.all(children);
        children.clear();
        assertEquals(1, requirement.children().size());
        try { requirement.children().clear(); fail("Mutable requirement tree"); }
        catch (UnsupportedOperationException expected) { }
    }
}
