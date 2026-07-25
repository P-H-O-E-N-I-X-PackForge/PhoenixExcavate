package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

@FunctionalInterface
public interface ShapeExpander {

    List<BlockPos> neighborsOf(BlockPos pos, BlockPos origin, Direction facing, boolean diagonals);
}
