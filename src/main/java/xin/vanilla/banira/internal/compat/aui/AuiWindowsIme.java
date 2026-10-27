package xin.vanilla.banira.internal.compat.aui;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFWNativeWin32;

final class AuiWindowsIme implements AutoCloseable {
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private Driver driver;
    private boolean unavailable;

    void update(long window, AuiTransientLayout.ImeArea area) {
        if (!WINDOWS || unavailable) return;
        try {
            if (area == null) { close(); return; }
            if (driver == null) driver = new Driver();
            driver.update(GLFWNativeWin32.glfwGetWin32Window(window), area);
        } catch (RuntimeException | LinkageError failure) {
            unavailable = true;
            try { close(); } catch (RuntimeException | LinkageError ignored) { }
            LogManager.getLogger().warn("Cannot position the AUI input method window", failure);
        }
    }

    @Override public void close() {
        Driver previous = driver; driver = null;
        if (previous == null) return;
        try { previous.close(); }
        catch (RuntimeException | LinkageError failure) {
            if (!unavailable) LogManager.getLogger().warn("Cannot restore the AUI input method position", failure);
            unavailable = true;
        }
    }

    public interface Imm extends StdCallLibrary {
        Pointer ImmGetContext(Pointer window);
        int ImmReleaseContext(Pointer window, Pointer context);
        int ImmGetCompositionWindow(Pointer context, Composition form);
        int ImmSetCompositionWindow(Pointer context, Composition form);
        int ImmGetCandidateWindow(Pointer context, int index, Candidate form);
        int ImmSetCandidateWindow(Pointer context, Candidate form);
    }

    @Structure.FieldOrder({"style", "x", "y", "left", "top", "right", "bottom"})
    public static class Composition extends Structure {
        public int style, x, y, left, top, right, bottom;
        Composition copy() {
            Composition result = new Composition();
            result.style = style; result.x = x; result.y = y; result.left = left;
            result.top = top; result.right = right; result.bottom = bottom; return result;
        }
        boolean same(Composition other) {
            return other != null && style == other.style && x == other.x && y == other.y && left == other.left
                    && top == other.top && right == other.right && bottom == other.bottom;
        }
    }

    @Structure.FieldOrder({"index", "style", "x", "y", "left", "top", "right", "bottom"})
    public static class Candidate extends Structure {
        public int index, style, x, y, left, top, right, bottom;
        Candidate copy() {
            Candidate result = new Candidate();
            result.index = index; result.style = style; result.x = x; result.y = y; result.left = left;
            result.top = top; result.right = right; result.bottom = bottom; return result;
        }
        boolean same(Candidate other) {
            return other != null && index == other.index && style == other.style && x == other.x && y == other.y && left == other.left
                    && top == other.top && right == other.right && bottom == other.bottom;
        }
    }

    private static final class Driver implements AutoCloseable {
        private final Imm api;
        private Pointer window, context;
        private boolean contextChecked;
        private Composition savedComposition, appliedComposition;
        private Candidate savedCandidate, appliedCandidate;

        Driver() { this(Native.load("imm32", Imm.class)); }
        Driver(Imm api) { this.api = api; }

        void update(long handle, AuiTransientLayout.ImeArea area) {
            if (handle == 0) { close(); return; }
            Pointer hwnd = Pointer.createConstant(handle), himc = api.ImmGetContext(hwnd);
            if (!contextChecked) {
                contextChecked = true;
                LogManager.getLogger().debug("AUI IME window={}, context={}, area={}", handle, himc, area);
            }
            if (himc == null) return;
            try {
                if (!hwnd.equals(window) || !himc.equals(context)) {
                    close(); window = hwnd; context = himc;
                    Composition original = new Composition(); Candidate candidate = new Candidate();
                    // The first composition may not expose a positioned form yet; restore CFS_DEFAULT in that case.
                    savedComposition = api.ImmGetCompositionWindow(himc, original) != 0 ? original.copy() : new Composition();
                    savedCandidate = api.ImmGetCandidateWindow(himc, 0, candidate) != 0 ? candidate.copy() : null;
                    LogManager.getLogger().debug("AUI IME saved composition={}, candidate={}", savedComposition, savedCandidate);
                }
                Composition wanted = new Composition(); wanted.style = 2; wanted.x = area.x(); wanted.y = area.y();
                Composition current = new Composition();
                boolean readComposition = api.ImmGetCompositionWindow(himc, current) != 0;
                if ((!readComposition || !wanted.same(current))
                        && api.ImmSetCompositionWindow(himc, wanted) != 0) appliedComposition = wanted;
                Candidate selection = new Candidate(); selection.style = 128; selection.x = area.x(); selection.y = area.y();
                selection.left = area.left(); selection.top = area.top(); selection.right = area.right(); selection.bottom = area.bottom();
                Candidate currentCandidate = new Candidate();
                boolean readCandidate = api.ImmGetCandidateWindow(himc, 0, currentCandidate) != 0;
                if (savedCandidate == null && readCandidate) savedCandidate = currentCandidate.copy();
                if (savedCandidate != null && (!readCandidate || !selection.same(currentCandidate))
                        && api.ImmSetCandidateWindow(himc, selection) != 0) appliedCandidate = selection;
            } finally { api.ImmReleaseContext(hwnd, himc); }
        }

        @Override public void close() {
            if (window == null) return;
            Pointer himc = api.ImmGetContext(window);
            if (himc != null) try {
                if (himc.equals(context)) {
                    Composition current = new Composition(); Candidate candidate = new Candidate();
                    if (appliedComposition != null && api.ImmGetCompositionWindow(himc, current) != 0 && appliedComposition.same(current))
                        api.ImmSetCompositionWindow(himc, savedComposition);
                    if (appliedCandidate != null && api.ImmGetCandidateWindow(himc, 0, candidate) != 0 && appliedCandidate.same(candidate))
                        api.ImmSetCandidateWindow(himc, savedCandidate);
                }
            } finally { api.ImmReleaseContext(window, himc); }
            window = context = null; savedComposition = appliedComposition = null; savedCandidate = appliedCandidate = null;
        }
    }
}
