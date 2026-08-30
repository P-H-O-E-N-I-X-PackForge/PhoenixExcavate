package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.phoenixvine.excavate.api.VeinShape;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinShapeRegistryTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    @BeforeEach
    void registerShapes() {
        VeinShapeRegistry.registerBuiltins();
    }

    private VeinShape shape(String id) {
        VeinShape s = VeinShapeRegistry.byId(id);
        assertNotNull(s, "expected a registered shape with id " + id);
        assertEquals(id, s.id());
        return s;
    }

    private Set<BlockPos> neighborsOf(VeinShape shape, BlockPos pos, BlockPos origin, Direction facing, boolean diagonals) {
        return shape.expander().neighborsOf(pos, origin, facing, diagonals).stream().collect(Collectors.toSet());
    }

    @Test
    void registerBuiltinsRegistersAllSixShapes() {
        List<VeinShape> all = VeinShapeRegistry.all();
        Set<String> ids = all.stream().map(VeinShape::id).collect(Collectors.toSet());
        assertEquals(Set.of("blob", "tunnel", "layer", "wall", "staircase", "shapeless"), ids);
    }

    @Test
    void registerBuiltinsClearsPreviousRegistrations() {
        VeinShapeRegistry.register(new VeinShape("custom", "custom.key", (pos, origin, facing, diagonals) -> List.of()));
        assertNotNull(VeinShapeRegistry.byId("custom"));

        VeinShapeRegistry.registerBuiltins();

        List<VeinShape> all = VeinShapeRegistry.all();
        assertFalse(all.stream().anyMatch(s -> s.id().equals("custom")), "registerBuiltins should wipe custom registrations");
        assertEquals(6, all.size());
    }

    @Test
    void byIdFallsBackToFirstRegisteredShapeForUnknownId() {
        VeinShape fallback = VeinShapeRegistry.byId("does-not-exist");
        assertEquals(VeinShapeRegistry.all().get(0), fallback);
    }

    @Test
    void blobWithoutDiagonalsReturnsTheSixFaceNeighbors() {
        VeinShape blob = shape("blob");
        Set<BlockPos> result = neighborsOf(blob, ORIGIN, ORIGIN, Direction.NORTH, false);

        assertEquals(Set.of(
                new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
                new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
                new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
        ), result);
    }

    @Test
    void blobWithDiagonalsReturnsAllTwentySixNeighbors() {
        VeinShape blob = shape("blob");
        Set<BlockPos> result = neighborsOf(blob, ORIGIN, ORIGIN, Direction.NORTH, true);

        assertEquals(26, result.size());
        assertFalse(result.contains(ORIGIN), "should not include the origin itself");
    }

    @Test
    void tunnelOnlyAdvancesOneStepInTheFacingDirection() {
        VeinShape tunnel = shape("tunnel");
        assertTrue(tunnel.traversesAir());

        Set<BlockPos> result = neighborsOf(tunnel, ORIGIN, ORIGIN, Direction.EAST, false);
        assertEquals(Set.of(new BlockPos(1, 0, 0)), result);

        result = neighborsOf(tunnel, ORIGIN, ORIGIN, Direction.DOWN, false);
        assertEquals(Set.of(new BlockPos(0, -1, 0)), result);
    }

    @Test
    void layerOnlyKeepsNeighborsAtTheOriginsYLevel() {
        VeinShape layer = shape("layer");
        BlockPos pos = new BlockPos(2, 1, 2);

        Set<BlockPos> result = neighborsOf(layer, pos, ORIGIN, Direction.NORTH, true);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(p -> p.getY() == ORIGIN.getY()));
    }

    @Test
    void wallFiltersOnTheAxisPerpendicularToFacing() {
        VeinShape wall = shape("wall");
        BlockPos pos = new BlockPos(3, 3, 3);

        Set<BlockPos> eastFacing = neighborsOf(wall, pos, ORIGIN, Direction.EAST, true);
        assertTrue(eastFacing.stream().allMatch(p -> p.getX() == ORIGIN.getX()));

        Set<BlockPos> upFacing = neighborsOf(wall, pos, ORIGIN, Direction.UP, true);
        assertTrue(upFacing.stream().allMatch(p -> p.getY() == ORIGIN.getY()));

        Set<BlockPos> southFacing = neighborsOf(wall, pos, ORIGIN, Direction.SOUTH, true);
        assertTrue(southFacing.stream().allMatch(p -> p.getZ() == ORIGIN.getZ()));
    }

    @Test
    void staircaseStepsDownAndForwardOnePairPerCall() {
        VeinShape staircase = shape("staircase");
        assertTrue(staircase.traversesAir());

        Set<BlockPos> result = neighborsOf(staircase, ORIGIN, ORIGIN, Direction.EAST, false);

        assertEquals(Set.of(new BlockPos(1, -1, 0), new BlockPos(1, 0, 0)), result);
    }

    @Test
    void staircaseReturnsNothingForPositionsOffTheStepLine() {
        VeinShape staircase = shape("staircase");
        BlockPos offLine = new BlockPos(5, 5, 5);

        Set<BlockPos> result = neighborsOf(staircase, offLine, ORIGIN, Direction.EAST, false);

        assertTrue(result.isEmpty());
    }

    @Test
    void shapelessLimitsLateralSpreadToTwoBlocksOnEachAxis() {
        VeinShape shapeless = shape("shapeless");
        BlockPos withinRange = new BlockPos(0, 2, 2);

        Set<BlockPos> result = neighborsOf(shapeless, withinRange, ORIGIN, Direction.EAST, true);
        assertFalse(result.isEmpty());
        for (BlockPos p : result) {
            assertTrue(Math.abs(p.getY() - ORIGIN.getY()) <= 2);
            assertTrue(Math.abs(p.getZ() - ORIGIN.getZ()) <= 2);
        }
    }
}
