package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VeinFinder {

    private VeinFinder() {}

    public static List<BlockPos> find(BlockGetter level, BlockPos origin, Direction facing,
                                      MatchMode matchMode, VeinShape shape) {
        List<BlockPos> result = new ArrayList<>();
        result.add(origin.immutable());
        if (matchMode == null || shape == null) return result;

        BlockState originState = level.getBlockState(origin);

        int cap = ExcavateServerConfig.effectiveMaxVeinSize();
        boolean diagonals = ExcavateSettings.get().isIncludeDiagonalNeighbors();

        int maxExplored = Math.max(cap * 8, 512);

        Set<BlockPos> visited = new LinkedHashSet<>();
        visited.add(origin.immutable());
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());

        while (!queue.isEmpty() && result.size() < cap && visited.size() < maxExplored) {
            BlockPos current = queue.poll();
            for (BlockPos next : shape.expander().neighborsOf(current, origin, facing, diagonals)) {
                if (visited.size() >= maxExplored) break;
                if (visited.contains(next)) continue;
                visited.add(next);

                BlockState candidate = level.getBlockState(next);

                if (shape.traversesAir() && candidate.isAir()) {
                    // Only let air-traversal continue through a face-adjacent (orthogonal) gap - a diagonal
                    // neighbor touching only at an edge/corner has no real path through it, so treating a
                    // diagonal air cell as passable would let the vein corner-cut through empty space between
                    // two blocks that aren't actually connected by a voxel-adjacent path.
                    int dx = Math.abs(next.getX() - current.getX());
                    int dy = Math.abs(next.getY() - current.getY());
                    int dz = Math.abs(next.getZ() - current.getZ());
                    if (dx + dy + dz > 1) continue;

                    queue.add(next);
                    continue;
                }

                if (!matchMode.matcher().matches(originState, candidate)) continue;

                result.add(next);
                queue.add(next);
                if (result.size() >= cap) break;
            }
        }

        return result;
    }
}
