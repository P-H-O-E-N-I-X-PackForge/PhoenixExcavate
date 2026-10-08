package net.phoenixvine.excavate.vein;

import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.api.MatchMode;

@FunctionalInterface
public interface VeinMatcher {

    boolean matches(BlockState origin, BlockState candidate);
}
