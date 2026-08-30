package net.phoenixvine.excavate.vein;

import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.api.MatchMode;


/**
 * This interface handles checking if adjacent blocks are part of the same vein
 * by checking the origin and the block path.
 *
 * @see MatchMode
 * @apiNote This interface is for internal use only.
 *
 */
@FunctionalInterface
public interface VeinMatcher {

    boolean matches(BlockState origin, BlockState candidate);
}
