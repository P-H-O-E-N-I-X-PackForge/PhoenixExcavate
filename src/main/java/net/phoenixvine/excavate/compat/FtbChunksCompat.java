package net.phoenixvine.excavate.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.fml.ModList;

public final class FtbChunksCompat {

    private static final boolean LOADED = ModList.get().isLoaded("ftbchunks");

    private FtbChunksCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean canBreak(ServerPlayer player, BlockPos pos) {
        if (!LOADED) return true;
        return !dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api().getManager().shouldPreventInteraction(
                player, InteractionHand.MAIN_HAND, pos, dev.ftb.mods.ftbchunks.api.Protection.EDIT_BLOCK, null);
    }
}
