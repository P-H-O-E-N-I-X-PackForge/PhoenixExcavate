package net.phoenixvine.excavate.vein.action;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinAction;
import net.phoenixvine.excavate.vein.VeinUtil;
import net.phoenixvine.excavate.vein.VeinMode;

import java.util.List;

/** In replace mode, right-clicking with a block swaps the whole clicked vein for that block. */
public final class ReplaceAction implements VeinAction {

    public static final ResourceLocation ID = PhoenixExcavate.id("replace");

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
        return ExcavateAPI.FEATURE_VEIN_REPLACE;
    }

    @Override
    public boolean appliesTo(Context context) {
        return context.mode() == VeinMode.REPLACE && context.held().getItem() instanceof BlockItem blockItem &&
                blockItem.getBlock() != context.originState().getBlock();
    }

    @Override
    public boolean includesOrigin() {
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
    public boolean applyTo(Context context, BlockPos pos) {
        ServerPlayer player = context.player();
        ServerLevel level = context.level();
        BlockItem blockItem = (BlockItem) context.held().getItem();

        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.getBlock() == blockItem.getBlock()) return false;
        if (VeinUtil.isUnbreakable(level, pos, existing)) return false;
        if (!ActionUtil.mayChange(player, level, pos)) return false;

        boolean charge = !player.getAbilities().instabuild;
        int slot = -1;
        if (charge) {
            slot = findSlot(player, context.held());
            if (slot < 0) return false;
        }

        BlockPos clicked = pos.relative(context.face().getOpposite());
        Vec3 hitLocation = Vec3.atCenterOf(clicked).add(context.face().getStepX() * 0.5,
                context.face().getStepY() * 0.5, context.face().getStepZ() * 0.5);
        BlockPlaceContext placeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                new ItemStack(blockItem), new BlockHitResult(hitLocation, context.face(), clicked, false));
        BlockState placed = blockItem.getBlock().getStateForPlacement(placeContext);
        if (placed == null) placed = blockItem.getBlock().defaultBlockState();
        if (!placed.canSurvive(level, pos)) return false;

        var blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(existing, level, pos, blockEntity, player, ItemStack.EMPTY);
        level.setBlock(pos, placed, Block.UPDATE_ALL);

        ActionUtil.deliver(player, level, pos, drops);
        SoundType sound = placed.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (charge) player.getInventory().getItem(slot).shrink(1);
        return true;
    }
}
