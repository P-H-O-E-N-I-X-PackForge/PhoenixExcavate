package net.phoenixvine.excavate.vein.action;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.phoenixvine.excavate.compat.ClaimProtectionCompat;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.List;

final class ActionUtil {

    private ActionUtil() {}

    static boolean mayChange(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return level.mayInteract(player, pos) && ClaimProtectionCompat.canBreak(player, pos);
    }

    static void deliver(ServerPlayer player, ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (ExcavateServerConfig.effectiveCollectToPlayer()) {
                player.getInventory().add(drop);
                if (!drop.isEmpty()) player.drop(drop, false);
            } else {
                Block.popResource(level, pos, drop);
            }
        }
    }

    static void exhaust(ServerPlayer player) {
        if (ExcavateServerConfig.effectiveRespectHunger()) player.causeFoodExhaustion(0.005F);
    }
}
