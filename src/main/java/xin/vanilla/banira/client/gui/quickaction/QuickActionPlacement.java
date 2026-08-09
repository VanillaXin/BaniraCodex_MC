package xin.vanilla.banira.client.gui.quickaction;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 以保存位置为首选，在排除区域边缘寻找最近的临时绘制位置。 */
public final class QuickActionPlacement {
    private QuickActionPlacement() {
    }

    public static QuickActionRect resolve(int preferredX, int preferredY, int width, int height,
                                          int screenWidth, int screenHeight,
                                          List<QuickActionRect> exclusions, int margin) {
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        int safeMargin = Math.max(0, margin);
        int minX = screenWidth >= safeWidth + safeMargin * 2 ? safeMargin : 0;
        int minY = screenHeight >= safeHeight + safeMargin * 2 ? safeMargin : 0;
        int maxX = Math.max(minX, screenWidth - safeWidth - minX);
        int maxY = Math.max(minY, screenHeight - safeHeight - minY);
        int clampedX = clamp(preferredX, minX, maxX);
        int clampedY = clamp(preferredY, minY, maxY);
        QuickActionRect fallback = new QuickActionRect(clampedX, clampedY, safeWidth, safeHeight);
        List<QuickActionRect> blocked = exclusions != null ? exclusions : Collections.emptyList();
        if (isAvailable(fallback, blocked, safeMargin)) {
            return fallback;
        }

        Set<Integer> xs = new LinkedHashSet<>();
        Set<Integer> ys = new LinkedHashSet<>();
        addCandidate(xs, clampedX, minX, maxX);
        addCandidate(ys, clampedY, minY, maxY);
        addCandidate(xs, minX, minX, maxX);
        addCandidate(ys, minY, minY, maxY);
        addCandidate(xs, maxX, minX, maxX);
        addCandidate(ys, maxY, minY, maxY);
        for (QuickActionRect obstacle : blocked) {
            if (obstacle == null || obstacle.isEmpty()) {
                continue;
            }
            addCandidate(xs, obstacle.x() - safeWidth - safeMargin, minX, maxX);
            addCandidate(xs, obstacle.right() + safeMargin, minX, maxX);
            addCandidate(ys, obstacle.y() - safeHeight - safeMargin, minY, maxY);
            addCandidate(ys, obstacle.bottom() + safeMargin, minY, maxY);
        }

        QuickActionRect best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int x : xs) {
            for (int y : ys) {
                QuickActionRect candidate = new QuickActionRect(x, y, safeWidth, safeHeight);
                if (!isAvailable(candidate, blocked, safeMargin)) {
                    continue;
                }
                long dx = (long) x - clampedX;
                long dy = (long) y - clampedY;
                long distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best != null ? best : fallback;
    }

    private static boolean isAvailable(QuickActionRect candidate, List<QuickActionRect> exclusions, int margin) {
        for (QuickActionRect exclusion : exclusions) {
            if (candidate.intersects(exclusion, margin)) {
                return false;
            }
        }
        return true;
    }

    private static void addCandidate(Set<Integer> values, int candidate, int min, int max) {
        values.add(clamp(candidate, min, max));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
