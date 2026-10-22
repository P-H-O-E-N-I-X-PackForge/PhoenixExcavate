package net.phoenixvine.excavate.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.vein.VeinMode;

/**
 * Something a player can do to a whole vein at once. Register one with {@link ExcavateAPI#registerAction}.
 *
 * <p>An action is picked when a player with vein actions active breaks a block ({@link Trigger#BREAK}) or right-clicks
 * one ({@link Trigger#USE}) and {@link #appliesTo} says yes. The vein is then found with the shape and match mode the
 * player chose, and {@link #applyTo} is called once per block, a few blocks per tick.
 */
public interface VeinAction {

    enum Trigger {
        /** The player broke a block. */
        BREAK,
        /** The player right-clicked a block. */
        USE
    }

    /** What the action is being asked about: the click that started it. */
    record Context(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, ItemStack held,
                   Direction face, VeinMode mode) {}

    ResourceLocation id();

    Trigger trigger();

    /** The {@link ExcavateAPI} feature id that can switch this action off, or null for none. */
    default String featureId() {
        return null;
    }

    boolean appliesTo(Context context);

    /** Does the action to one block of the vein. Returns true if it did something. */
    boolean applyTo(Context context, BlockPos pos);

    /**
     * True if the clicked block is part of the action too, which also cancels the vanilla break or use. False if
     * vanilla is left to handle the clicked block itself and the action covers only the rest of the vein.
     */
    default boolean includesOrigin() {
        return false;
    }

    /** True if only blocks of the same type as the clicked one can be part of the vein (crops, for example). */
    default boolean sameBlockOnly() {
        return false;
    }

    /** Checked between blocks: return false to stop the rest of the vein (the tool broke, the items ran out). */
    default boolean canContinue(Context context) {
        return true;
    }
}
