package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.compat.ClaimProtectionCompat;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The right-click vein actions: harvesting (and replanting) crops, and repeating a tool's use (tilling, stripping,
 * flattening, shearing, waxing) across a vein. Mining and placing keep their own handlers; these share one job queue
 * that is drained a few blocks per tick, like the others.
 */
@EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public final class VeinActions {

    private VeinActions() {}

    /** Does one block of a job. Returns true if it did something. */
    private interface Step {
        boolean apply(ServerPlayer player, ServerLevel level, BlockPos pos);
    }

    private static final class Job {
        final Deque<BlockPos> remaining;
        final Step step;
        final boolean toolJob;

        Job(List<BlockPos> targets, Step step, boolean toolJob) {
            this.remaining = new ArrayDeque<>(targets);
            this.step = step;
            this.toolJob = toolJob;
        }
    }

    private static final Map<UUID, Deque<Job>> JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        if (active == null) return;
        if (active.mode() != VeinMode.HARVEST && active.mode() != VeinMode.USE) return;

        try {
            if (active.mode() == VeinMode.HARVEST) {
                handleHarvest(event, player, level, active);
            } else {
                handleUse(event, player, level, active);
            }
        } catch (Throwable t) {
            PhoenixExcavate.LOGGER.error("Vein action {} failed for {} at {}: the click itself is unaffected.",
                    active.mode(), player.getName().getString(), event.getPos(), t);
        }
    }

    // ---- harvesting ----

    static boolean isMatureCrop(BlockState state) {
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

    private static void handleHarvest(PlayerInteractEvent.RightClickBlock event, ServerPlayer player,
                                      ServerLevel level, VeinServerState.Active active) {
        BlockPos origin = event.getPos();
        BlockState originState = level.getBlockState(origin);
        if (!isMatureCrop(originState)) return;

        ItemStack held = player.getMainHandItem();
        if (held.is(Items.BONE_MEAL)) return;

        if (!ExcavateAPI.isFeatureEnabled(ExcavateAPI.FEATURE_VEIN_HARVEST, level.dimension().location(), player,
                held)) {
            return;
        }

        var config = VeinUtil.getValidatedConfig(active);
        if (config == null || debounced(player, level)) return;

        List<BlockPos> vein = VeinFinder.find(level, origin, FacingUtil.facingOf(player.getLookAngle()),
                config.matchMode(), config.shape(), held);

        Block cropBlock = originState.getBlock();
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos pos : vein) {
            if (level.getBlockState(pos).getBlock() == cropBlock) targets.add(pos);
        }
        if (targets.isEmpty()) return;

        BlockPos first = targets.remove(0);
        boolean did = harvestOne(player, level, first);
        if (did) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
        if (!targets.isEmpty()) {
            JOBS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>())
                    .add(new Job(targets, VeinActions::harvestOne, false));
        }
    }

    private static boolean harvestOne(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isMatureCrop(state)) return false;
        if (!level.mayInteract(player, pos) || !ClaimProtectionCompat.canBreak(player, pos)) return false;

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

        level.setBlock(pos, replant ? replanted(state) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 2,
                0.25, 0.2, 0.25, 0.0);

        if (ExcavateServerConfig.effectiveCollectToPlayer()) {
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;
                player.getInventory().add(drop);
                if (!drop.isEmpty()) player.drop(drop, false);
            }
        } else {
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) Block.popResource(level, pos, drop);
            }
        }

        if (ExcavateServerConfig.effectiveRespectHunger()) player.causeFoodExhaustion(0.005F);
        return true;
    }

    // ---- repeating a tool's use ----

    static boolean isUsableTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof DiggerItem || stack.getItem() instanceof ShearsItem ||
                stack.getItem() instanceof HoneycombItem || stack.getItem() instanceof BrushItem) {
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return ExcavateServerConfig.USE_EXTRA_ITEMS.get().contains(id);
    }

    private static void handleUse(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, ServerLevel level,
                                  VeinServerState.Active active) {
        ItemStack held = player.getMainHandItem();
        if (!isUsableTool(held)) return;

        if (!ExcavateAPI.isFeatureEnabled(ExcavateAPI.FEATURE_VEIN_USE, level.dimension().location(), player, held)) {
            return;
        }

        var config = VeinUtil.getValidatedConfig(active);
        if (config == null || debounced(player, level)) return;

        BlockPos origin = event.getPos();
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        MatchMode matchMode = config.matchMode();
        VeinShape shape = config.shape();

        List<BlockPos> vein = new ArrayList<>(VeinFinder.find(level, origin,
                FacingUtil.facingOf(player.getLookAngle()), matchMode, shape, held));
        if (!vein.isEmpty()) vein.remove(0);
        if (vein.isEmpty()) return;

        JOBS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>())
                .add(new Job(vein, (p, l, pos) -> useOne(p, l, pos, face), true));
    }

    private static boolean useOne(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face) {
        ItemStack stack = player.getMainHandItem();
        if (!isUsableTool(stack)) return false;
        if (!level.mayInteract(player, pos) || !ClaimProtectionCompat.canBreak(player, pos)) return false;

        Vec3 hitPos = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, face, pos, false);
        InteractionResult result = stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
        if (!result.consumesAction()) return false;

        if (ExcavateServerConfig.effectiveRespectHunger()) player.causeFoodExhaustion(0.005F);
        return true;
    }

    // ---- job queue ----

    private static boolean debounced(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        Long last = LAST_TRIGGER_TICK.get(player.getUUID());
        if (last != null && now - last < ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get()) return true;
        LAST_TRIGGER_TICK.put(player.getUUID(), now);
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) return;

        int budget = ExcavateServerConfig.VEIN_BLOCKS_PER_TICK.get();
        var server = ServerLifecycleHooks.getCurrentServer();

        JOBS.entrySet().removeIf(entry -> {
            Deque<Job> queue = entry.getValue();
            Job job = queue.peek();
            if (job == null) return true;

            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                queue.clear();
                return true;
            }

            int processed = 0;
            while (processed < budget && !job.remaining.isEmpty()) {
                BlockPos pos = job.remaining.poll();
                processed++;
                try {
                    job.step.apply(player, level, pos);
                } catch (Throwable t) {
                    PhoenixExcavate.LOGGER.error("Vein action step failed at {}", pos, t);
                }
                if (job.toolJob && !isUsableTool(player.getMainHandItem())) job.remaining.clear();
            }

            if (job.remaining.isEmpty()) queue.poll();
            return queue.isEmpty();
        });
    }
}
