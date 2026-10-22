package net.phoenixvine.excavate.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.vein.VeinMode;

public interface VeinAction {

    enum Trigger {
        
        BREAK,
        
        USE
    }

    record Context(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, ItemStack held,
                   Direction face, VeinMode mode) {}

    ResourceLocation id();

    Trigger trigger();

    default String featureId() {
        return null;
    }

    boolean appliesTo(Context context);

    boolean applyTo(Context context, BlockPos pos);

    default boolean includesOrigin() {
        return false;
    }

    default boolean sameBlockOnly() {
        return false;
    }

    default boolean canContinue(Context context) {
        return true;
    }
}
