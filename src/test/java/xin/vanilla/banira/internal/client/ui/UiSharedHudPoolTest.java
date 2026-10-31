package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UiSharedHudPoolTest {
    private static final class Document {
        boolean valid = true;
        int closed;
    }

    private static final class Branch {
        boolean shown, removed;
    }

    private static final class Port implements UiSharedHudPool.Platform<Document, Branch> {
        final List<Document> documents = new ArrayList<>();
        int shows, hides;
        boolean failBranch, failRemove, failClose, failShow;

        public Document open() {
            Document document = new Document();
            documents.add(document);
            return document;
        }

        public boolean current(Document document) {
            return document.valid && document.closed == 0;
        }

        public Branch branch(Document document) {
            if (failBranch) throw new IllegalStateException("branch");
            return new Branch();
        }

        public void show(Document document, Branch branch) {
            assertFalse(branch.removed);
            if (failShow) throw new IllegalStateException("show");
            branch.shown = true;
            shows++;
        }

        public void hide(Document document, Branch branch) {
            branch.shown = false;
            hides++;
        }

        public void remove(Document document, Branch branch) {
            branch.removed = true;
            if (failRemove) throw new IllegalStateException("remove");
        }

        public void close(Document document) {
            document.closed++;
            document.valid = false;
            if (failClose) throw new IllegalStateException("close");
        }
    }

    @Test
    public void branchesShareOneDocumentAndSelectOnlyWhenThePhaseChanges() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        lombok.var second = pool.acquire();
        assertSame(first.document(), second.document());
        assertNotSame(first.branch(), second.branch());
        first.show();
        first.show();
        assertEquals(1, port.shows);
        assertEquals(0, port.hides);
        second.show();
        assertFalse(first.branch().shown);
        assertTrue(second.branch().shown);
        assertEquals(2, port.shows);
        assertEquals(1, port.hides);
        first.close();
        assertEquals(0, second.document().closed);
        assertTrue(second.branch().shown);
        second.close();
        second.close();
        assertEquals(1, second.document().closed);
    }

    @Test
    public void removingTheSelectedBranchDoesNotClaimOrCloseItsSibling() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        lombok.var second = pool.acquire();
        first.show();
        first.close();
        assertFalse(second.branch().removed);
        assertEquals(0, second.document().closed);
        second.show();
        assertTrue(second.branch().shown);
        second.close();
    }

    @Test
    public void staleGenerationCannotDisposeTheReplacement() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var old = pool.acquire();
        old.document().valid = false;
        lombok.var next = pool.acquire();
        assertNotSame(old.document(), next.document());
        assertThrows(IllegalStateException.class, old::show);
        old.close();
        assertEquals(1, old.document().closed);
        assertEquals(0, next.document().closed);
        next.show();
        assertTrue(next.branch().shown);
        next.close();
    }

    @Test
    public void failedFirstAcquireReleasesTheUnownedDocument() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        port.failBranch = true;
        assertThrows(IllegalStateException.class, pool::acquire);
        assertEquals(1, port.documents.get(0).closed);
        port.failBranch = false;
        lombok.var next = pool.acquire();
        assertEquals(2, port.documents.size());
        next.close();
    }

    @Test
    public void failedAdditionalAcquirePreservesTheExistingOwner() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        port.failBranch = true;
        assertThrows(IllegalStateException.class, pool::acquire);
        assertEquals(0, first.document().closed);
        first.show();
        first.close();
    }

    @Test
    public void removeFailureStillReleasesTheLastDocumentAndResetsThePool() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        port.failRemove = true;
        assertThrows(IllegalStateException.class, first::close);
        assertEquals(1, first.document().closed);
        port.failRemove = false;
        lombok.var next = pool.acquire();
        assertNotSame(first.document(), next.document());
        next.close();
    }

    @Test
    public void documentCloseFailureDoesNotPoisonTheNextGeneration() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        port.failClose = true;
        assertThrows(IllegalStateException.class, first::close);
        port.failClose = false;
        lombok.var next = pool.acquire();
        assertNotSame(first.document(), next.document());
        next.close();
    }

    @Test
    public void closedLeaseCannotSelectOrDisturbItsSibling() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        lombok.var second = pool.acquire();
        first.close();
        second.show();
        int shows = port.shows, hides = port.hides;
        assertThrows(IllegalStateException.class, first::show);
        assertEquals(shows, port.shows);
        assertEquals(hides, port.hides);
        assertTrue(second.branch().shown);
        second.close();
    }

    @Test
    public void sharingIsExplicitAndLegacyConstructorsRemainExclusive() {
        UiHudBinding binding = new UiHudBinding() {
            public boolean visible() {
                return true;
            }

            public void bind(UiView view) {
            }

            public void detach(UiDetachReason reason) {
            }
        };
        assertFalse(new UiHudSpec("test:one", "test/one.html", binding).shared());
        assertFalse(new UiHudSpec("test:two", "test/two.html", binding, true).shared());
        UiHudSpec shared = new UiHudSpec("test:three", "test/three.html", binding, true, true);
        assertTrue(shared.manual());
        assertTrue(shared.shared());
    }

    @Test
    public void failedSelectionAllowsThePreviousOwnerToRestoreItsBranch() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        lombok.var second = pool.acquire();
        first.show();
        port.failShow = true;
        assertThrows(IllegalStateException.class, second::show);
        second.close();
        port.failShow = false;
        first.show();
        assertTrue(first.branch().shown);
        first.close();
    }

    @Test
    public void bothCleanupFailuresAreRetainedAndTheDocumentIsStillReleased() {
        Port port = new Port();
        UiSharedHudPool<Document, Branch> pool = new UiSharedHudPool<>(port);
        lombok.var first = pool.acquire();
        port.failRemove = port.failClose = true;
        IllegalStateException failure = assertThrows(IllegalStateException.class, first::close);
        assertEquals("remove", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("close", failure.getSuppressed()[0].getMessage());
        assertEquals(1, first.document().closed);
    }
}
