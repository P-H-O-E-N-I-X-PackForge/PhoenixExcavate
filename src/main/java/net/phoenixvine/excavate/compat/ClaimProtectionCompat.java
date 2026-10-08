package net.phoenixvine.excavate.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class ClaimProtectionCompat {

    private ClaimProtectionCompat() {}

    public static boolean canBreak(ServerPlayer player, BlockPos pos) {
        return FtbChunksCompat.canBreak(player, pos) && PhoenixDomainsCompat.canBreak(player, pos);
    }

    public static boolean canPlace(ServerPlayer player, BlockPos pos) {
        return canBreak(player, pos);
    }
}
