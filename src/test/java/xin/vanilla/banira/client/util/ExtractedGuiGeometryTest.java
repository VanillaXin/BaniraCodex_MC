package xin.vanilla.banira.client.util;

import org.joml.Matrix3x2f;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static xin.vanilla.banira.client.util.ExtractedGuiRenderer.Primitive.*;

public class ExtractedGuiGeometryTest {
    @Test
    public void triangleKeepsThreePointsAndDuplicatesTheLastPoint() {
        var batch = ExtractedGuiRenderer.begin(TRIANGLES);
        var pose = new Matrix3x2f();
        batch.vertex(pose, 0, 0, 0x40112233);
        batch.vertex(pose, 0, 2, 0x80556677);
        batch.vertex(pose, 3, 2, 0xFF99AABB);
        var quads = batch.quads();
        assertEquals(4, quads.size());
        assertEquals(0x40112233, quads.get(0).color());
        assertEquals(0x80556677, quads.get(1).color());
        assertEquals(0xFF99AABB, quads.get(2).color());
        assertEquals(quads.get(2), quads.get(3));
        assertEquals(3f, Math.abs(area(quads)), 0f);
    }

    @Test
    public void triangleStripKeepsEveryTriangleAndConsistentWinding() {
        var batch = ExtractedGuiRenderer.begin(TRIANGLE_STRIP);
        var pose = new Matrix3x2f();
        batch.vertex(pose, 0, 0, -1);
        batch.vertex(pose, 0, 2, -1);
        batch.vertex(pose, 3, 0, -1);
        batch.vertex(pose, 3, 2, -1);
        var quads = batch.quads();
        assertEquals(8, quads.size());
        assertEquals(-3f, area(quads.subList(0, 4)), 0f);
        assertEquals(-3f, area(quads.subList(4, 8)), 0f);
        assertEquals(quads.get(2), quads.get(3));
        assertEquals(quads.get(6), quads.get(7));
    }

    @Test
    public void transformedVerticesAreSnapshotsIncludingMirroredQuads() {
        var batch = ExtractedGuiRenderer.begin(QUADS);
        var pose = new Matrix3x2f().translate(10, 20).scale(-2, 3);
        batch.vertex(pose, 0, 0, 0x10FFFFFF);
        batch.vertex(pose, 0, 2, 0x20FFFFFF);
        batch.vertex(pose, 3, 2, 0x30FFFFFF);
        batch.vertex(pose, 3, 0, 0x40FFFFFF);
        pose.identity();
        var quads = batch.quads();
        assertEquals(4, quads.size());
        assertEquals(10f, quads.get(0).x(), 0f);
        assertEquals(20f, quads.get(0).y(), 0f);
        assertEquals(4f, quads.get(1).x(), 0f);
        assertEquals(20f, quads.get(1).y(), 0f);
        assertEquals(0x40FFFFFF, quads.get(1).color());
        assertTrue(area(quads) < 0);
    }

    @Test
    public void fractionalBoundsEncloseAllGeometry() {
        var rect = ExtractedGuiRenderer.bounds(List.of(
                new ExtractedGuiRenderer.Vertex(-.25f, 1.25f, -1),
                new ExtractedGuiRenderer.Vertex(3.5f, 4.75f, -1)));
        assertEquals(-1, rect.left());
        assertEquals(1, rect.top());
        assertEquals(5, rect.width());
        assertEquals(4, rect.height());
    }

    @Test(expected = IllegalStateException.class)
    public void incompleteTriangleFailsInsteadOfSilentlyDroppingVertices() {
        var batch = ExtractedGuiRenderer.begin(TRIANGLES);
        batch.vertex(new Matrix3x2f(), 0, 0, -1);
        batch.quads();
    }

    private static float area(List<ExtractedGuiRenderer.Vertex> quad) {
        var a = quad.get(0);
        var b = quad.get(1);
        var c = quad.get(2);
        return ((b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x())) / 2f;
    }
}
