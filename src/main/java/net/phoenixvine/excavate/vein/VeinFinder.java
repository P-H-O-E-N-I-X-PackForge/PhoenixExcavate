package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;

import java.util.*;

public final class VeinFinder {

    private VeinFinder() {}


    private static final int WALL_DEPTH_TOLERANCE = 1;
    private static final int STAIRCASE_STEP_TOLERANCE = 1;

    public static List<BlockPos> find(BlockGetter level, BlockPos origin, Direction facing,
                                      MatchMode matchMode, VeinShape shape, ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos startPos = origin.immutable();
        result.add(startPos);

        if (matchMode == null || shape == null) return result;

        BlockState originState = level.getBlockState(startPos);
        if (isUnbreakable(level, startPos, originState)) return result;

        int cap = ExcavateServerConfig.effectiveMaxVeinSize();
        int maxExplored = Math.max(cap * 8, 512);

        ExcavateSettings settings = ExcavateSettings.get();
        boolean diagonals = settings.isIncludeDiagonalNeighbors();
        boolean toolTierEnabled = settings.isRespectToolTier();
        boolean isShapeless = "shapeless".equals(shape.id());

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty() && result.size() < cap && visited.size() < maxExplored) {
            BlockPos current = queue.poll();

            for (BlockPos next : shape.expander().neighborsOf(current, origin, facing, diagonals)) {
                // Consolidated bounds guard check
                if (result.size() >= cap || visited.size() >= maxExplored) break;
                if (!visited.add(next)) continue;

                BlockState candidate = level.getBlockState(next);

                if (isUnbreakable(level, next, candidate)) continue;

                if (toolTierEnabled && candidate.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(candidate)) {
                    continue;
                }

                if (candidate.isAir()) {
                    if (shape.traversesAir() && !isManhattanDistanceGreaterThanOne(current, next)) {
                        queue.add(next);
                    }
                    continue;
                }

                if (!matchMode.matcher().matches(originState, candidate)) continue;
                if (isShapeless && !BlockFamily.sameFamily(originState, candidate)) continue;

                result.add(next);
                queue.add(next);
            }
        }

        return result;
    }

    public static List<BlockPos> findForPlacement(BlockGetter level, BlockPos bfsStart, BlockState anchorState,
                                                  Direction facing, MatchMode matchMode, VeinShape shape) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos startPos = bfsStart.immutable();
        result.add(startPos);

        if (matchMode == null || shape == null) return result;

        int cap = ExcavateServerConfig.effectiveMaxVeinSize();
        int maxExplored = Math.max(cap * 8, 512);

        ExcavateSettings settings = ExcavateSettings.get();
        boolean diagonals = settings.isIncludeDiagonalNeighbors();
        boolean replaceMatching = ExcavateServerConfig.effectivePlaceReplacesMatching();

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty() && result.size() < cap && visited.size() < maxExplored) {
            BlockPos current = queue.poll();

            for (BlockPos next : placementNeighborsOf(shape, current, bfsStart, facing, diagonals)) {
                if (result.size() >= cap || visited.size() >= maxExplored) break;

                BlockPos target = next.immutable();
                if (!visited.add(target)) continue;

                BlockState candidate = level.getBlockState(target);
                boolean onPlane = isOnShapePlane(shape, target, bfsStart, facing);

                if (shape.traversesAir() && candidate.canBeReplaced()) {
                    if (onPlane) {
                        result.add(target);
                    }
                    queue.add(target);
                    continue;
                }

                if (!isUnbreakable(level, target, candidate) && matchMode.matcher().matches(anchorState, candidate)) {
                    if (replaceMatching && onPlane) {
                        result.add(target);
                    }
                    queue.add(target);
                }
            }
        }

        return result;
    }


    private static List<BlockPos> placementNeighborsOf(VeinShape shape, BlockPos pos, BlockPos origin,
                                                        Direction facing, boolean diagonals) {
        return switch (shape.id()) {
            case "wall" -> allNeighbors(pos, diagonals).stream()
                    .filter(p -> switch (facing.getAxis()) {
                        case X -> Math.abs(p.getX() - origin.getX()) <= WALL_DEPTH_TOLERANCE;
                        case Y -> Math.abs(p.getY() - origin.getY()) <= WALL_DEPTH_TOLERANCE;
                        case Z -> Math.abs(p.getZ() - origin.getZ()) <= WALL_DEPTH_TOLERANCE;
                    })
                    .toList();
            case "staircase" -> allNeighbors(pos, diagonals).stream()
                    .filter(p -> {
                        int yOffset = p.getY() - origin.getY();
                        int horizOffset = (p.getX() - origin.getX()) * facing.getStepX() +
                                (p.getZ() - origin.getZ()) * facing.getStepZ();
                        return Math.abs(yOffset + horizOffset) <= STAIRCASE_STEP_TOLERANCE;
                    })
                    .toList();
            default -> shape.expander().neighborsOf(pos, origin, facing, diagonals);
        };
    }


    private static boolean isOnShapePlane(VeinShape shape, BlockPos pos, BlockPos origin, Direction facing) {
        return switch (shape.id()) {
            case "wall" -> switch (facing.getAxis()) {
                case X -> pos.getX() == origin.getX();
                case Y -> pos.getY() == origin.getY();
                case Z -> pos.getZ() == origin.getZ();
            };
            case "staircase" -> {
                int yOffset = pos.getY() - origin.getY();
                int horizOffset = (pos.getX() - origin.getX()) * facing.getStepX() +
                        (pos.getZ() - origin.getZ()) * facing.getStepZ();
                yield yOffset + horizOffset == 0;
            }
            default -> true;
        };
    }

    private static final BlockPos[] FACE_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private static List<BlockPos> allNeighbors(BlockPos pos, boolean diagonals) {
        if (!diagonals) {
            return java.util.Arrays.stream(FACE_OFFSETS).map(pos::offset).toList();
        }
        return BlockPos.betweenClosedStream(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))
                .filter(p -> !p.equals(pos))
                .map(BlockPos::immutable)
                .toList();
    }

    static boolean isUnbreakable(BlockGetter level, BlockPos pos, BlockState state) {
        return state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0.0F;
    }

    private static boolean isManhattanDistanceGreaterThanOne(BlockPos a, BlockPos b) {
        int dx = Math.abs(b.getX() - a.getX());
        int dy = Math.abs(b.getY() - a.getY());
        int dz = Math.abs(b.getZ() - a.getZ());
        return (dx + dy + dz) > 1;
    }
}
