package net.phoenixvine.excavate.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class ClaimProtectionCompat {

    private ClaimProtectionCompat() {}

    public static boolean canBreak(ServerPlayer player, BlockPos pos) {
        return FtbChunksCompat.canBreak(player, pos) && PhoenixDomainsCompat.canBreak(player, pos);
    }

    // FtbChunksCompat/PhoenixDomainsCompat's "canBreak" checks are already generic edit/interact
    // permission checks (Protection.EDIT_BLOCK, DomainAPI.canInteract), not break-specific, so
    // they're the correct pre-check to reuse for placement too rather than duplicating them.
    public static boolean canPlace(ServerPlayer player, BlockPos pos) {
        return canBreak(player, pos);
    }
}
