package xin.vanilla.banira.client.notification;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class NotificationTypedMuteStateTest {
    private final AtomicLong clock = new AtomicLong(100);
    private final NotificationMuteState state = new NotificationMuteState(clock::get);
    private Object call(NotificationMuteState s, String name, String type) throws Exception {
        Method m = null;
        for (Method candidate : NotificationMuteState.class.getDeclaredMethods()) {
            if (candidate.getName().equals(name) && candidate.getParameterCount() == 1) m = candidate;
        }
        assertNotNull("Per-type mute API: " + name, m);
        return m.invoke(s, type);
    }
    private int mute(String type) throws Exception { return (Integer) call(state, "mute", type); }
    private int next(String type) throws Exception { return (Integer) call(state, "nextMinutes", type); }
    private boolean muted(String type) throws Exception { return (Boolean) call(state, "muted", type); }
    private void advance(long minutes) { clock.addAndGet(TimeUnit.MINUTES.toNanos(minutes)); }

    @Test public void onlySelectedTypeIsMuted() throws Exception {
        assertEquals(5, mute("a")); assertTrue(muted("a")); assertFalse(muted("b"));
        assertEquals(5, mute("b")); assertTrue(muted("a")); assertTrue(muted("b"));
    }
    @Test public void expiresExactlyAtFirstDeadline() throws Exception {
        mute("a"); clock.addAndGet(TimeUnit.MINUTES.toNanos(5)-1);
        assertTrue(muted("a")); clock.incrementAndGet(); assertFalse(muted("a"));
    }
    @Test public void repeatsInsideWindowFollowAllTiersAndCap() throws Exception {
        for (int minutes : new int[]{5,10,30,60,120,240,480,960,960}) {
            assertEquals(minutes, next("a")); assertEquals(minutes, mute("a"));
            advance(minutes); assertFalse(muted("a"));
        }
    }
    @Test public void repeatWhileMutedDoesNotExtendDeadline() throws Exception {
        mute("a"); advance(4); assertEquals(5,mute("a"));
        advance(1); assertFalse(muted("a")); assertEquals(10,next("a"));
    }
    @Test public void exactWindowEndReturnsToFirstTier() throws Exception {
        mute("a"); advance(10); assertFalse(muted("a"));
        assertEquals(5,next("a")); assertEquals(5,mute("a"));
    }
    @Test public void lastNanosecondInWindowStillEscalates() throws Exception {
        mute("a"); clock.addAndGet(TimeUnit.MINUTES.toNanos(10)-1);
        assertEquals(10,next("a")); clock.incrementAndGet(); assertEquals(5,next("a"));
    }
    @Test public void lateFirstObservationDoesNotStartWindowLate() throws Exception {
        mute("a"); advance(11); assertFalse(muted("a")); assertEquals(5,mute("a"));
    }
    @Test public void recoveryWindowStaysFiveMinutesAtCap() throws Exception {
        for (int minutes : new int[]{5,10,30,60,120,240,480}) {mute("a");advance(minutes);}
        assertEquals(960,mute("a")); advance(964); assertEquals(960,next("a"));
        advance(1); assertEquals(5,next("a"));
    }
    @Test public void normalizedEmptyTypeUsesDefaultOnly() throws Exception {
        mute(null); assertTrue(muted(" ")); assertTrue(muted("default"));
        assertFalse(muted("a")); assertEquals(5,next("a"));
    }
    @Test public void tierIsIndependentBetweenTypes() throws Exception {
        mute("a"); advance(5); assertEquals(10,mute("a"));
        assertEquals(5,next("b")); assertEquals(5,mute("b"));
    }
    @Test public void previewDoesNotMuteOrAdvanceTier() throws Exception {
        mute("a"); advance(5);
        for(int i=0;i<50;i++)assertEquals(10,next("a"));
        assertFalse(muted("a")); assertEquals(10,mute("a"));
        advance(10); assertEquals(30,next("a"));
    }
    @Test public void nanoTimeOverflowKeepsRelativeDeadlines() throws Exception {
        clock.set(Long.MAX_VALUE-50); mute("a");
        clock.addAndGet(TimeUnit.MINUTES.toNanos(5)-1); assertTrue(muted("a"));
        clock.incrementAndGet(); assertFalse(muted("a")); assertEquals(10,next("a"));
    }
    @Test public void newClientStateHasNeitherMuteNorTier() throws Exception {
        mute("a"); advance(5); mute("a");
        NotificationMuteState fresh=new NotificationMuteState(clock::get);
        assertFalse((Boolean)call(fresh,"muted","a")); assertEquals(5,call(fresh,"nextMinutes","a"));
    }
}
