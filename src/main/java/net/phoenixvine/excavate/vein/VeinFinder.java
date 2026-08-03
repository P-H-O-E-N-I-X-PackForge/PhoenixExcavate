package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
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

        if (originState.getDestroySpeed(level, origin) < 0.0F || originState.is(Blocks.BEDROCK)) {
            return result;
        }

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

                if (candidate.is(Blocks.BEDROCK) || candidate.getDestroySpeed(level, next) < 0.0F) {
                    continue;
                }

                if (shape.traversesAir() && candidate.isAir()) {
                    int dx = Math.abs(next.getX() - current.getX());
                    int dy = Math.abs(next.getY() - current.getY());
                    int dz = Math.abs(next.getZ() - current.getZ());
                    if (dx + dy + dz > 1) continue;

                    queue.add(next);
                    continue;
                }

                if (!matchMode.matcher().matches(originState, candidate)) continue;

                if (shape.id().equals("shapeless") && !BlockFamily.sameFamily(originState, candidate)) continue;

                result.add(next);
                queue.add(next);
                if (result.size() >= cap) break;
            }
        }

        return result;
    }
}