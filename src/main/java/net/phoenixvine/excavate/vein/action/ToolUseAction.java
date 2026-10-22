package net.phoenixvine.excavate.vein.action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinAction;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.vein.VeinMode;

public final class ToolUseAction implements VeinAction {

    public static final ResourceLocation ID = PhoenixExcavate.id("tool_use");

    public static boolean isUsable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof DiggerItem || stack.getItem() instanceof ShearsItem ||
                stack.getItem() instanceof HoneycombItem || stack.getItem() instanceof BrushItem ||
                stack.getItem() instanceof BoneMealItem) {
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return ExcavateServerConfig.USE_EXTRA_ITEMS.get().contains(id);
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
        return ExcavateAPI.FEATURE_VEIN_USE;
    }

    @Override
    public boolean appliesTo(Context context) {
        return context.mode() == VeinMode.PLACE && isUsable(context.held());
    }

    @Override
    public boolean canContinue(Context context) {
        return isUsable(context.player().getMainHandItem());
    }

    @Override
    public boolean applyTo(Context context, BlockPos pos) {
        ServerPlayer player = context.player();
        ServerLevel level = context.level();
        ItemStack stack = player.getMainHandItem();
        if (!isUsable(stack) || !ActionUtil.mayChange(player, level, pos)) return false;

        var face = context.face();
        Vec3 hitPos = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, face, pos, false);
        InteractionResult result = stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
        if (!result.consumesAction()) return false;

        ActionUtil.exhaust(player);
        return true;
    }
}
