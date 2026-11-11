package xin.vanilla.banira.api.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.lang.reflect.Modifier;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiDownstreamContractTest {
    @Test
    public void sakuraPublicEntryPointsRemainAvailableWithCompletion() throws Exception {
        var artwork = UiGameElement.class.getMethod("artwork", DropdownOption.class);
        assertTrue(Modifier.isStatic(artwork.getModifiers()));
        assertEquals(UiGameElement.class, artwork.getReturnType());
        assertEquals(InputFormScreen.Widget.class,
                InputFormScreen.Widget.class.getMethod("pickerReadOnly", boolean.class).getReturnType());
        assertEquals(InputFormScreen.Widget.class,
                InputFormScreen.Widget.class.getMethod("visibleWhen", Predicate.class).getReturnType());
        assertEquals(InputFormScreen.Widget.class,
                InputFormScreen.Widget.class.getMethod("completion", UiTextCompletionProvider.class).getReturnType());
        assertEquals(AutoCloseable.class,
                UiView.class.getMethod("forwardScroll", String.class, String.class, BooleanSupplier.class).getReturnType());
        assertEquals(void.class, UiPageState.class.getMethod("listPosition", String.class, double.class).getReturnType());
        assertEquals(double.class, UiPageState.class.getMethod("listPosition", String.class).getReturnType());
    }

    @Test
    public void poolScrollPositionsStayIndependentAndFinite() {
        var state = new UiPageState();
        state.listPosition("pool_rows", 7);
        state.listPosition("reward_rows", 12);
        state.listPosition("invalid", Double.NaN);
        assertEquals(7, state.listPosition("pool_rows"), 0);
        assertEquals(12, state.listPosition("reward_rows"), 0);
        assertEquals(0, state.listPosition("invalid"), 0);
        assertEquals(0, state.listPosition("missing"), 0);
    }
}
