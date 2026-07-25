package net.phoenixvine.excavate.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class PhoenixDomainsCompat {

    private static final boolean LOADED = ModList.get().isLoaded("phoenix_domains");

    private PhoenixDomainsCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean canBreak(ServerPlayer player, BlockPos pos) {
        if (!LOADED) return true;
        return net.phoenixvine.domains.api.DomainAPI.canInteract(player, pos);
    }
}
