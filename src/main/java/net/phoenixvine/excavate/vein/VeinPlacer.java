package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.compat.ClaimProtectionCompat;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.*;


@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public class VeinPlacer {

    private static final class Job {
        final BlockPos origin;
        final Deque<BlockPos> remaining;
        final BlockItem blockItem;
        final Direction face;
        final BlockState anchorState;
        final MatchMode matchMode;
        int placed = 0;

        Job(BlockPos origin, List<BlockPos> candidates, BlockItem blockItem,
            Direction face, BlockState anchorState,
            MatchMode matchMode) {
            this.origin = origin;
            this.remaining = new ArrayDeque<>(candidates);
            this.blockItem = blockItem;
            this.face = face;
            this.anchorState = anchorState;
            this.matchMode = matchMode;
        }
    }

    private static final Map<UUID, Deque<Job>> JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        try {
            handleRightClick(event, player, level);
        } catch (Throwable t) {
            PhoenixExcavate.LOGGER.error(
                    "Vein-placing failed for {} at {}: the triggering placement is unaffected.",
                    player.getName().getString(), event.getPos(), t);
        }
    }

    private static void handleRightClick(PlayerInteractEvent.RightClickBlock event, ServerPlayer player,
                                         ServerLevel level) {
        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        if (active == null || active.mode() != VeinMode.PLACE) return;

        ItemStack heldStack = player.getItemInHand(event.getHand());
        if (!(heldStack.getItem() instanceof BlockItem blockItem)) return;

        if (!ExcavateAPI.isFeatureEnabled(ExcavateAPI.FEATURE_VEIN_PLACING, level.dimension().location(), player,
                heldStack)) {
            return;
        }

        if (!ExcavateServerConfig.isShapeAllowed(active.shapeId().toString()) ||
                !ExcavateServerConfig.isMatchModeAllowed(active.matchModeId().toString())) {
            return;
        }

        VeinShape shape = VeinShapeRegistry.byId(active.shapeId().toString());
        MatchMode matchMode = MatchModeRegistry.byId(active.matchModeId().toString());
        if (shape == null || matchMode == null) return;

        long now = level.getGameTime();
        Long lastTrigger = LAST_TRIGGER_TICK.get(player.getUUID());
        int debounceTicks = ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get();
        if (lastTrigger != null && now - lastTrigger < debounceTicks) return;

        BlockPos anchorPos = event.getPos();
        BlockState anchorState = level.getBlockState(anchorPos);
        if (anchorState.isAir()) return;

        Direction face = event.getFace();
        Direction facing = FacingUtil.facingOf(player.getLookAngle());

        assert face != null;
        BlockPos firstTarget = anchorPos.relative(face);
        if (!level.getBlockState(firstTarget).canBeReplaced()) return;

        List<BlockPos> targets = VeinFinder.findForPlacement(level, firstTarget, anchorState, facing, matchMode,
                shape);
        if (targets.isEmpty()) return;

        LAST_TRIGGER_TICK.put(player.getUUID(), now);


        targets.remove(0);
        if (targets.isEmpty()) return;

        JOBS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>())
                .add(new Job(firstTarget, targets, blockItem, face, anchorState, matchMode));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (JOBS.isEmpty()) return;
        drainJobs();
    }

    @SuppressWarnings("Duplicates")
    private static void drainJobs() {
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

            drainOneJob(player, level, job, budget);

            if (job.remaining.isEmpty()) queue.poll();
            return queue.isEmpty();
        });
    }

    private static void drainOneJob(ServerPlayer player, ServerLevel level, Job job, int budget) {
        boolean consumeInventory = ExcavateServerConfig.effectivePlaceConsumesInventory() &&
                !player.getAbilities().instabuild;

        int processed = 0;
        while (processed < budget && !job.remaining.isEmpty()) {
            BlockPos pos = job.remaining.poll();
            processed++;

            int invSlot = -1;
            if (consumeInventory) {
                invSlot = findMatchingStackSlot(player, job.blockItem);
                if (invSlot < 0) {
                    job.remaining.clear();
                    break;
                }
            }

            BlockState existing = level.getBlockState(pos);
            boolean replacingSolid = !existing.canBeReplaced();
            if (replacingSolid && (VeinFinder.isUnbreakable(level, pos, existing) ||
                    !ExcavateServerConfig.effectivePlaceReplacesMatching() ||
                    !job.matchMode.matcher().matches(job.anchorState, existing))) {
                continue;
            }

            if (!ClaimProtectionCompat.canPlace(player, pos)) continue;

            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot, existing, player);
            MinecraftForge.EVENT_BUS.post(placeEvent);
            if (placeEvent.isCanceled()) continue;


            if (replacingSolid) {
                var blockEntity = level.getBlockEntity(pos);
                if (ExcavateServerConfig.effectiveCollectToPlayer()) {
                    List<ItemStack> drops = Block.getDrops(existing, level, pos, blockEntity, player, ItemStack.EMPTY);
                    giveToPlayer(player, drops);
                } else {
                    Block.dropResources(existing, level, pos, blockEntity, player, ItemStack.EMPTY);
                }
            }


            BlockState placedState = computePlacementState(player, job.blockItem, pos, job.face);

            level.setBlockAndUpdate(pos, placedState);
            SoundType soundType = placedState.getSoundType();
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            if (invSlot >= 0) {
                player.getInventory().getItem(invSlot).shrink(1);
            }

            job.placed++;
        }
    }

    private static BlockState computePlacementState(ServerPlayer player, BlockItem blockItem, BlockPos pos,
                                                      Direction face) {
        BlockPos clickedPos = pos.relative(face.getOpposite());
        Vec3 hitLocation = Vec3.atCenterOf(clickedPos)
                .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitLocation, face, clickedPos, false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(blockItem),
                hit);
        BlockState state = blockItem.getBlock().getStateForPlacement(context);
        return state != null ? state : blockItem.getBlock().defaultBlockState();
    }

    private static void giveToPlayer(ServerPlayer player, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            player.getInventory().add(drop);
            if (!drop.isEmpty()) {
                player.drop(drop, false);
            }
        }
    }

    private static int findMatchingStackSlot(ServerPlayer player, Item item) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        return -1;
    }
}
