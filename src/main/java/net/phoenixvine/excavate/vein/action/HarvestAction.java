package net.phoenixvine.excavate.vein.action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinAction;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.vein.VeinMode;

import java.util.List;

public final class HarvestAction implements VeinAction {

    public static final ResourceLocation ID = PhoenixExcavate.id("harvest");

    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        if (block instanceof SweetBerryBushBlock) return state.getValue(SweetBerryBushBlock.AGE) >= 3;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= 2;
        return false;
    }

    private static BlockState replanted(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getStateForAge(0);
        if (block instanceof NetherWartBlock) return state.setValue(NetherWartBlock.AGE, 0);
        if (block instanceof SweetBerryBushBlock) return state.setValue(SweetBerryBushBlock.AGE, 1);
        if (block instanceof CocoaBlock) return state.setValue(CocoaBlock.AGE, 0);
        return state;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Trigger trigger() {
        return Trigger.BREAK;
    }

    @Override
    public String featureId() {
        return ExcavateAPI.FEATURE_VEIN_HARVEST;
    }

    @Override
    public boolean appliesTo(Context context) {
        return context.mode() == VeinMode.MINE && isMatureCrop(context.originState());
    }

    @Override
    public boolean includesOrigin() {
        return true;
    }

    @Override
    public boolean sameBlockOnly() {
        return true;
    }

    @Override
    public boolean applyTo(Context context, BlockPos pos) {
        ServerPlayer player = context.player();
        ServerLevel level = context.level();
        BlockState state = level.getBlockState(pos);
        if (!isMatureCrop(state) || !ActionUtil.mayChange(player, level, pos)) return false;

        ItemStack tool = ExcavateServerConfig.effectiveRespectEnchantments() ? player.getMainHandItem() :
                ItemStack.EMPTY;
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);

        boolean replant = ExcavateServerConfig.HARVEST_REPLANTS.get();
        if (replant && state.getBlock() instanceof CropBlock) {
            ItemStack seed = state.getBlock().getCloneItemStack(level, pos, state);
            for (ItemStack drop : drops) {
                if (!drop.isEmpty() && drop.is(seed.getItem())) {
                    drop.shrink(1);
                    break;
                }
            }
        }

        level.setBlock(pos, replant ? replanted(state) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 2,
                0.25, 0.2, 0.25, 0.0);

        ActionUtil.deliver(player, level, pos, drops);
        ActionUtil.exhaust(player);
        return true;
    }
}
