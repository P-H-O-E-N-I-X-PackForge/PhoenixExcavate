package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.ArrayList;
import java.util.List;

public final class VeinShapeRegistry {

    private static final List<VeinShape> SHAPES = new ArrayList<>();
    private static final int SHAPELESS_LATERAL_LIMIT = 2;

    private VeinShapeRegistry() {}

    public static void registerBuiltins() {
        SHAPES.clear();

        SHAPES.add(new VeinShape("blob", "phoenix_excavate.shape.blob",
                VeinShapeRegistry::getBlobPositions));
        SHAPES.add(new VeinShape("tunnel", "phoenix_excavate.shape.tunnel",
                VeinShapeRegistry::getTunnelPositions, true));
        SHAPES.add(new VeinShape("layer", "phoenix_excavate.shape.layer",
                VeinShapeRegistry::getLayerPositions, true));
        SHAPES.add(new VeinShape("wall", "phoenix_excavate.shape.wall",
                VeinShapeRegistry::getWallPositions, true));
        SHAPES.add(new VeinShape("staircase", "phoenix_excavate.shape.staircase",
                VeinShapeRegistry::getStaircasePositions, true));
        SHAPES.add(new VeinShape("shapeless", "phoenix_excavate.shape.shapeless",
                VeinShapeRegistry::elongatedOffsetPositions, true));
    }

    private static List<BlockPos> getBlobPositions(BlockPos pos, BlockPos origin, Direction facing,
                                                   boolean diagonals) {
        return allOffsetPositions(pos, diagonals);
    }

    private static List<BlockPos> getTunnelPositions(BlockPos pos, BlockPos origin, Direction facing,
                                                     boolean diagonals) {
        return List.of(pos.relative(facing));
    }

    private static List<BlockPos> getLayerPositions(BlockPos pos, BlockPos origin, Direction facing,
                                                    boolean diagonals) {
        List<BlockPos> all = allOffsetPositions(pos, diagonals);
        List<BlockPos> out = new ArrayList<>(all.size());
        for (BlockPos p : all) if (p.getY() == origin.getY()) out.add(p);
        return out;
    }

    private static List<BlockPos> getWallPositions(BlockPos pos, BlockPos origin, Direction facing,
                                                   boolean diagonals) {
        List<BlockPos> all = allOffsetPositions(pos, diagonals);
        List<BlockPos> out = new ArrayList<>(all.size());
        switch (facing.getAxis()) {
            case X -> { for (BlockPos p : all) if (p.getX() == origin.getX()) out.add(p); }
            case Y -> { for (BlockPos p : all) if (p.getY() == origin.getY()) out.add(p); }
            case Z -> { for (BlockPos p : all) if (p.getZ() == origin.getZ()) out.add(p); }
        }
        return out;
    }

    private static List<BlockPos> getStaircasePositions(BlockPos pos, BlockPos origin,
                                                        Direction facing, boolean diagonals) {
        int yOffset = pos.getY() - origin.getY();
        int horizOffset = (pos.getX() - origin.getX()) * facing.getStepX() +
                (pos.getZ() - origin.getZ()) * facing.getStepZ();
        if (yOffset + horizOffset != 0) return List.of();

        BlockPos nextFloor = pos.relative(facing).below();
        return List.of(nextFloor, nextFloor.above());
    }

    public static void register(VeinShape shape) {
        SHAPES.removeIf(s -> s.id().equals(shape.id()));
        SHAPES.add(shape);
    }

    private static final BlockPos[] FACE_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    public static List<VeinShape> all() {
        return List.copyOf(SHAPES);
    }

    public static List<VeinShape> allEnabled() {
        List<VeinShape> out = new ArrayList<>();
        for (VeinShape s : SHAPES) if (ExcavateServerConfig.isShapeAllowed(s.id())) out.add(s);
        return out;
    }

    private static String path(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    public static VeinShape byId(String id) {
        String path = path(id);
        for (VeinShape s : SHAPES) if (s.id().equals(path)) return s;
        return SHAPES.isEmpty() ? null : SHAPES.get(0);
    }

    public static VeinShape next(String currentId) {
        List<VeinShape> enabled = allEnabled();
        if (enabled.isEmpty()) return null;

        String path = path(currentId);
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).id().equals(path)) {
                return enabled.get((i + 1) % enabled.size());
            }
        }

        return enabled.get(0);
    }

    private static List<BlockPos> elongatedOffsetPositions(BlockPos pos, BlockPos origin,
                                                           Direction facing, boolean diagonals) {
        List<BlockPos> all = allOffsetPositions(pos, diagonals);
        Direction.Axis axis = facing.getAxis();

        List<BlockPos> out = new ArrayList<>(all.size());
        for (BlockPos candidate : all) {
            int dx = candidate.getX() - origin.getX();
            int dy = candidate.getY() - origin.getY();
            int dz = candidate.getZ() - origin.getZ();

            boolean keep = switch (axis) {
                case X -> Math.abs(dy) <= SHAPELESS_LATERAL_LIMIT && Math.abs(dz) <= SHAPELESS_LATERAL_LIMIT;
                case Y -> Math.abs(dx) <= SHAPELESS_LATERAL_LIMIT && Math.abs(dz) <= SHAPELESS_LATERAL_LIMIT;
                case Z -> Math.abs(dx) <= SHAPELESS_LATERAL_LIMIT && Math.abs(dy) <= SHAPELESS_LATERAL_LIMIT;
            };
            if (keep) out.add(candidate);
        }
        return out;
    }

    private static List<BlockPos> allOffsetPositions(BlockPos pos, boolean diagonals) {
        if (!diagonals) {
            List<BlockPos> out = new ArrayList<>(FACE_OFFSETS.length);
            for (BlockPos offset : FACE_OFFSETS) out.add(pos.offset(offset));
            return out;
        }
        List<BlockPos> out = new ArrayList<>(26);
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    out.add(new BlockPos(px + dx, py + dy, pz + dz));
                }
            }
        }
        return out;
    }
}
