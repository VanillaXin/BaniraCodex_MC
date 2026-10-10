package xin.vanilla.banira.api;

import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.*;

public class SharedCostPortContractTest {
    private Class<?> costs() {
        try { return Class.forName("xin.vanilla.banira.api.BaniraCosts"); }
        catch (ClassNotFoundException absent) { fail("Shared cost API is missing from this maintenance version"); return null; }
    }
    @Test public void compositeRequirementsAreAvailableWithoutNativePlayerInitialization() throws Exception {
        Object requirement=costs().getMethod("parse",String.class).invoke(null,"xpPoints(2) && score(\"draw\",1)");
        assertEquals("ALL",requirement.getClass().getMethod("kind").invoke(requirement).toString());
    }
    @Test public void invalidAlternativeCannotAuthorizeFreePayment() throws Exception {
        try {
            costs().getMethod("parse",String.class).invoke(null,"xpPoints(2) || System.exit(0)");
            fail("Invalid payment alternative was accepted");
        } catch (InvocationTargetException failure) { assertTrue(failure.getCause() instanceof IllegalArgumentException); }
    }
}
