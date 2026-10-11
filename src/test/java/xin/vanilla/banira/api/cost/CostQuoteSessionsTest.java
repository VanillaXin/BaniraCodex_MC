package xin.vanilla.banira.api.cost;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CostQuoteSessionsTest {
    @Test
    public void eightBurstTokensRefillAtOnePerQuarterSecond() {
        Fixture f = new Fixture();
        f.service.open(f.player, f.session);
        for (int i = 1; i <= 8; i++) assertEquals(CostFailure.NONE, f.accept(i));
        assertEquals(CostFailure.RATE_LIMITED, f.accept(9));
        f.now = 249_999_999L;
        assertEquals(CostFailure.RATE_LIMITED, f.accept(9));
        f.now++;
        assertEquals(CostFailure.NONE, f.accept(9));
        assertEquals(CostFailure.RATE_LIMITED, f.accept(10));
    }

    @Test
    public void openingSameSessionDoesNotResetBudgetOrSequence() {
        Fixture f = new Fixture();
        f.service.open(f.player, f.session);
        assertEquals(CostFailure.NONE, f.accept(1));
        f.service.open(f.player, f.session);
        assertEquals(CostFailure.INVALID_OPERATION, f.accept(1));
        for (int i = 2; i <= 7; i++) assertEquals(CostFailure.NONE, f.accept(i));
        assertEquals(CostFailure.RATE_LIMITED, f.accept(8));
    }

    @Test
    public void newSessionRejectsOldSessionAndResetsLocalQuoteSequence() {
        Fixture f = new Fixture();
        f.service.open(f.player, f.session);
        assertEquals(CostFailure.NONE, f.accept(1));
        UUID replacement = new UUID(0, 3);
        f.service.open(f.player, replacement);
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(2));
        assertEquals(CostFailure.NONE, f.service.accept(f.player, replacement, 1, f.generation));
    }

    @Test
    public void staleGenerationConsumesRequestButCannotAuthorizeIt() {
        Fixture f = new Fixture();
        f.service.open(f.player, f.session);
        assertEquals(CostFailure.CONFIGURATION_UNAVAILABLE, f.service.accept(f.player, f.session, 1, 0));
        assertEquals(CostFailure.INVALID_OPERATION, f.accept(1));
        assertEquals(CostFailure.NONE, f.accept(2));
    }

    @Test
    public void disconnectedAndClosedSessionsCannotBeReused() {
        Fixture f = new Fixture();
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(1));
        f.service.open(f.player, f.session);
        f.connected = false;
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(1));
        f.connected = true;
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(1));
        f.service.open(f.player, f.session);
        f.service.close(f.player);
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(1));
        f.service.open(f.player, f.session);
        f.service.clear();
        assertEquals(CostFailure.ACTOR_UNAVAILABLE, f.accept(1));
    }

    @Test
    public void backwardsClockCannotRefillTokens() {
        Fixture f = new Fixture();
        f.now = 1_000_000_000L;
        f.service.open(f.player, f.session);
        for (int i = 1; i <= 8; i++) assertEquals(CostFailure.NONE, f.accept(i));
        f.now = 0;
        assertEquals(CostFailure.RATE_LIMITED, f.accept(9));
        f.now = 1_249_999_999L;
        assertEquals(CostFailure.RATE_LIMITED, f.accept(9));
        f.now++;
        assertEquals(CostFailure.NONE, f.accept(9));
    }

    @Test
    public void crossThreadDoesNotConsumeTokenOrRequest() throws Exception {
        Fixture f = new Fixture();
        f.service.open(f.player, f.session);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try { f.accept(1); } catch (Throwable e) { error.set(e); }
        });
        thread.start();
        thread.join();
        assertTrue(error.get() instanceof IllegalStateException);
        assertEquals(CostFailure.NONE, f.accept(1));
    }

    private static final class Fixture {
        final UUID player = new UUID(0, 1), session = new UUID(0, 2);
        long now, generation = 1;
        boolean connected = true;
        final CostQuoteSessions service = new CostQuoteSessions(() -> now, () -> generation,
                id -> connected && player.equals(id));
        CostFailure accept(long request) { return service.accept(player, session, request, generation); }
    }
}
