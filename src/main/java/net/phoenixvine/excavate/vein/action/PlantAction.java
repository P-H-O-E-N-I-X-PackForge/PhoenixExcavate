package net.phoenixvine.excavate.vein.action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinAction;
import net.phoenixvine.excavate.vein.VeinMode;

public final class PlantAction implements VeinAction {

    public static final ResourceLocation ID = PhoenixExcavate.id("plant");

    public static boolean isPlantable(Item item) {
        if (!(item instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        return block instanceof CropBlock || block instanceof NetherWartBlock || block instanceof SaplingBlock ||
                block instanceof StemBlock;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Trigger trigger() {
        return Trigger.USE;
    }

    @Override
    public String featureId() {
        return ExcavateAPI.FEATURE_VEIN_PLANT;
    }

    @Override
    public boolean appliesTo(Context context) {
        return context.mode() == VeinMode.PLACE && context.face() == Direction.UP &&
                isPlantable(context.held().getItem());
    }

    @Override
    public boolean sameBlockOnly() {
        return true;
    }

    @Override
    public boolean canContinue(Context context) {
        return context.player().getAbilities().instabuild || findSlot(context.player(), context.held()) >= 0;
    }

    private static int findSlot(ServerPlayer player, ItemStack wanted) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() == wanted.getItem()) return i;
        }
        return -1;
    }

    @Override
    public boolean applyTo(Context context, BlockPos soil) {
        ServerPlayer player = context.player();
        ServerLevel level = context.level();
        BlockItem blockItem = (BlockItem) context.held().getItem();
        BlockPos target = soil.above();

        if (!level.getBlockState(target).canBeReplaced() || !level.getFluidState(target).isEmpty()) return false;
        if (!ActionUtil.mayChange(player, level, target)) return false;

        boolean charge = !player.getAbilities().instabuild;
        int slot = -1;
        if (charge) {
            slot = findSlot(player, context.held());
            if (slot < 0) return false;
        }

        Vec3 hitLocation = Vec3.atCenterOf(soil).add(0, 0.5, 0);
        BlockPlaceContext placeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                new ItemStack(blockItem), new BlockHitResult(hitLocation, Direction.UP, soil, false));
        BlockState state = blockItem.getBlock().getStateForPlacement(placeContext);
        if (state == null || !state.canSurvive(level, target)) return false;

        level.setBlock(target, state, Block.UPDATE_ALL);
        SoundType sound = state.getSoundType();
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (charge) player.getInventory().getItem(slot).shrink(1);
        return true;
    }
}
