package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.ArrayList;
import java.util.List;

public final class VeinShapeRegistry {

    private static final BlockPos[] FACE_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private static final List<VeinShape> SHAPES = new ArrayList<>();

    private VeinShapeRegistry() {}

    public static void registerBuiltins() {
        SHAPES.clear();

        SHAPES.add(new VeinShape("blob", "phoenix_excavate.shape.blob",
                (pos, origin, facing, diagonals) -> allOffsetPositions(pos, diagonals)));

        SHAPES.add(new VeinShape("tunnel", "phoenix_excavate.shape.tunnel",
                (pos, origin, facing, diagonals) -> List.of(pos.relative(facing)), true));

        SHAPES.add(new VeinShape("layer", "phoenix_excavate.shape.layer",
                (pos, origin, facing, diagonals) -> allOffsetPositions(pos, diagonals).stream()
                        .filter(p -> p.getY() == origin.getY())
                        .toList(), true));

        SHAPES.add(new VeinShape("wall", "phoenix_excavate.shape.wall",
                (pos, origin, facing, diagonals) -> {
                    List<BlockPos> all = allOffsetPositions(pos, diagonals);
                    Direction.Axis axis = facing.getAxis();
                    if (axis == Direction.Axis.Y) {
                        return all.stream().filter(p -> p.getY() == origin.getY()).toList();
                    } else if (axis == Direction.Axis.X) {
                        return all.stream().filter(p -> p.getX() == origin.getX()).toList();
                    } else {
                        return all.stream().filter(p -> p.getZ() == origin.getZ()).toList();
                    }
                }, true));

        SHAPES.add(new VeinShape("staircase", "phoenix_excavate.shape.staircase",
                (pos, origin, facing, diagonals) -> {
                    // Each step advances the floor by 1 forward + 1 down, but also includes the block directly
                    // above that new floor so the staircase is actually walkable (2 vertical blocks per step),
                    // not just a 1-wide diagonal line. That headroom block must NOT itself keep expanding the
                    // staircase - only cells on the true floor line do - otherwise the headroom line would
                    // recursively spawn its own parallel staircase one level up, forever. We distinguish floor
                    // vs. headroom purely from pos/origin/facing (no extra state needed): on the floor line,
                    // vertical offset from origin always equals -(forward distance); headroom cells are exactly
                    // 1 higher than that.
                    int yOffset = pos.getY() - origin.getY();
                    int horizOffset = (pos.getX() - origin.getX()) * facing.getStepX() +
                            (pos.getZ() - origin.getZ()) * facing.getStepZ();
                    if (yOffset + horizOffset != 0) return List.of();

                    BlockPos nextFloor = pos.relative(facing).below();
                    return List.of(nextFloor, nextFloor.above());
                }, true));

        SHAPES.add(new VeinShape("shapeless", "phoenix_excavate.shape.shapeless",
                (pos, origin, facing, diagonals) -> allOffsetPositions(pos, diagonals), true));
    }

    public static void register(VeinShape shape) {
        SHAPES.removeIf(s -> s.id().equals(shape.id()));
        SHAPES.add(shape);
    }

    public static List<VeinShape> all() {
        return List.copyOf(SHAPES);
    }

    public static List<VeinShape> allEnabled() {
        List<VeinShape> out = new ArrayList<>();
        for (VeinShape s : SHAPES) if (ExcavateServerConfig.isShapeAllowed(s.id())) out.add(s);
        return out;
    }

    public static VeinShape byId(String id) {
        for (VeinShape s : SHAPES) if (s.id().equals(id)) return s;
        return SHAPES.isEmpty() ? null : SHAPES.get(0);
    }

    public static VeinShape next(String currentId) {
        List<VeinShape> enabled = allEnabled();
        if (enabled.isEmpty()) return null;
        int idx = 0;
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).id().equals(currentId)) {
                idx = i;
                break;
            }
        }
        return enabled.get((idx + 1) % enabled.size());
    }

    private static List<BlockPos> allOffsetPositions(BlockPos pos, boolean diagonals) {
        if (!diagonals) {
            List<BlockPos> out = new ArrayList<>(FACE_OFFSETS.length);
            for (BlockPos o : FACE_OFFSETS) out.add(pos.offset(o));
            return out;
        }
        List<BlockPos> out = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    out.add(pos.offset(dx, dy, dz));
                }
            }
        }
        return out;
    }
}
