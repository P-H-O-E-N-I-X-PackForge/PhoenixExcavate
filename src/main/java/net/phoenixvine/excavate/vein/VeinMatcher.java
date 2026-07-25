package net.phoenixvine.excavate.vein;

import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface VeinMatcher {

    boolean matches(BlockState origin, BlockState candidate);
}
