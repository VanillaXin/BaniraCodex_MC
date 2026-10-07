package xin.vanilla.banira.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorldCoordinateBlockGeometryTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void integerComponentsMatchContainingBlockForNegativeFractions() {
        for (double value : new double[]{-.01, -.5, -1.01, -16.1, -64.75, 0, .5, 16.1}) {
            WorldCoordinate coordinate = new WorldCoordinate(value, value, value);
            assertContainingBlock(coordinate);
        }
    }

    @Test
    public void adjacentRepresentableValuesKeepTheCorrectSideOfIntegerBoundaries() {
        for (double boundary : new double[]{-29999984, -64, -16, -1, 0, 1, 16, 64, 29999984}) {
            for (double value : new double[]{Math.nextDown(boundary), boundary, Math.nextUp(boundary)}) {
                WorldCoordinate coordinate = new WorldCoordinate(value, value, value);
                assertContainingBlock(coordinate);
                assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits(coordinate.x()));
                assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits(coordinate.y()));
                assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits(coordinate.z()));
            }
        }
    }

    @Test
    public void chunkIndicesMatchTheContainingBlockOnBothSidesOfZeroAndChunkEdges() {
        for (double value : new double[]{-32.1, -32, -16.1, -16, -.1, 0, 15.9, 16, 16.1}) {
            WorldCoordinate coordinate = new WorldCoordinate(value, 70, value);
            BlockPos block = coordinate.toBlockPos();
            assertEquals("x=" + value, block.getX() >> 4, coordinate.chunkX());
            assertEquals("z=" + value, block.getZ() >> 4, coordinate.chunkZ());
            assertEquals((block.getX() >> 4) + "," + (block.getZ() >> 4), coordinate.chunkXZString());
        }
    }

    @Test
    public void rangeComparisonUsesBlockCellsAcrossZeroAndOnEveryAxis() {
        WorldCoordinate negative = new WorldCoordinate(-.5, -.5, -.5);
        assertTrue(negative.equalsInRange(new WorldCoordinate(-.9, -.9, -.9), 0));
        assertFalse(negative.equalsInRange(new WorldCoordinate(.5, -.5, -.5), 0));
        assertFalse(negative.equalsInRange(new WorldCoordinate(-.5, .5, -.5), 0));
        assertFalse(negative.equalsInRange(new WorldCoordinate(-.5, -.5, .5), 0));
        assertFalse(negative.equalsInRange(new WorldCoordinate(-1.01, -.5, -.5), 0));
        assertTrue(negative.equalsInRange(new WorldCoordinate(.5, .5, .5), 1));
        assertFalse(negative.equalsInRange(new WorldCoordinate(.5, .5, .5, Level.NETHER), 1));
    }

    @Test
    public void blockProjectionAndCloneDoNotRoundSavedTeleportCoordinates() {
        WorldCoordinate coordinate = new WorldCoordinate(-.123456789, -42.875, -16.999999999, 32.125, -12.5);
        WorldCoordinate copy = coordinate.clone();
        assertContainingBlock(copy);
        assertEquals(coordinate.x(), copy.x(), 0);
        assertEquals(coordinate.y(), copy.y(), 0);
        assertEquals(coordinate.z(), copy.z(), 0);
        assertEquals(coordinate.yaw(), copy.yaw(), 0);
        assertEquals(coordinate.pitch(), copy.pitch(), 0);
    }

    private static void assertContainingBlock(WorldCoordinate coordinate) {
        BlockPos block = coordinate.toBlockPos();
        assertEquals("x=" + coordinate.x(), block.getX(), coordinate.xInt());
        assertEquals("y=" + coordinate.y(), block.getY(), coordinate.yInt());
        assertEquals("z=" + coordinate.z(), block.getZ(), coordinate.zInt());
    }
}
