package xin.vanilla.banira.internal.client.minimap;

public final class MinimapLineLayout {
    private MinimapLineLayout() {
    }

    public static float[] positions(float anchorY, float[] heights, float screenHeight, boolean above) {
        float total = Math.max(0, heights.length - 1) * 2f;
        for (float height : heights) total += height;
        float start = above ? anchorY - total : anchorY;
        start = Math.max(0, Math.min(start, screenHeight - total));
        float[] positions = new float[heights.length];
        for (int i = 0; i < heights.length; i++) {
            positions[i] = start;
            start += heights[i] + 2;
        }
        return positions;
    }
}
