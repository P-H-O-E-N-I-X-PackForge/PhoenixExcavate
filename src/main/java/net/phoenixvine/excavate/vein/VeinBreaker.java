package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;

import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.api.event.VeinMineEvent;
import net.phoenixvine.excavate.compat.ClaimProtectionCompat;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public class VeinBreaker {

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    private static final class Job {
        final BlockPos origin;
        final Deque<BlockPos> remaining;
        int broken = 0;

        Job(BlockPos origin, List<BlockPos> candidates) {
            this.origin = origin;
            this.remaining = new ArrayDeque<>(candidates);
        }
    }

    private static final Map<UUID, Deque<Job>> JOBS = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || Boolean.TRUE.equals(PROCESSING.get())) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        
        try {
            PROCESSING.set(true);
            handleBreak(event, player, level);
        } catch (Throwable t) {
            logFailure(player, event, t);
        } finally {
            PROCESSING.set(false);
        }
    }

    private static void logFailure(ServerPlayer player, BlockEvent.BreakEvent event, Throwable t) {
        
        String playerName = (player != null) ? player.getName().getString() : "Unknown Player";
        String posString = (event != null && event.getPos() != null) ? event.getPos().toString() : "Unknown Pos";

        try {
            PhoenixExcavate.LOGGER.error("Vein-mining failed for {} at {} - the triggering block break is unaffected.", playerName, posString, t);
        } catch (Throwable loggingFailure) {
            System.err.println("[PhoenixExcavate] Vein-mining failed for " + playerName + " at " + posString +
                    " (Logger failed: " + loggingFailure.getMessage() + t + ")");
        }
    }

    private static void handleBreak(BlockEvent.BreakEvent event, ServerPlayer player, ServerLevel level) {
        ExcavateSettings settings = ExcavateSettings.get();

        if (settings.isGeneralMiningExhaustion()) {
            player.causeFoodExhaustion(0.005F);
        }

        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        if (active == null) return;

        ItemStack heldTool = player.getMainHandItem();

        if (!ExcavateAPI.isFeatureEnabled(ExcavateAPI.FEATURE_VEIN_MINING, level.dimension().location(), player,
                heldTool)) {
            return;
        }

        if (!ExcavateServerConfig.hasRequiredEnchantment(heldTool)) {
            return;
        }

        if (!ExcavateServerConfig.isShapeAllowed(active.shapeId()) ||
                !ExcavateServerConfig.isMatchModeAllowed(active.matchModeId())) {
            return;
        }

        VeinShape shape = VeinShapeRegistry.byId(active.shapeId());
        MatchMode matchMode = MatchModeRegistry.byId(active.matchModeId());
        if (shape == null || matchMode == null) return;

        long now = level.getGameTime();
        Long lastTrigger = LAST_TRIGGER_TICK.get(player.getUUID());
        int debounceTicks = ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get();
        if (lastTrigger != null && now - lastTrigger < debounceTicks) return;
        LAST_TRIGGER_TICK.put(player.getUUID(), now);

        BlockPos origin = event.getPos();
        Direction facing = FacingUtil.facingOf(player.getLookAngle());
        List<BlockPos> vein = VeinFinder.find(level, origin, facing, matchMode, shape);
        if (vein.size() <= 1) return;

        List<BlockPos> extras = new ArrayList<>(vein.subList(1, vein.size()));

        VeinMineEvent.Pre pre = new VeinMineEvent.Pre(player, origin, extras);
        MinecraftForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;
        if (pre.getCandidates().isEmpty()) return;

        BlockState originState = level.getBlockState(origin);
        SoundType soundType = originState.getSoundType();
        level.playSound(null, origin, soundType.getBreakSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        JOBS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).add(new Job(origin, pre.getCandidates()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (JOBS.isEmpty()) return;

        try {
            PROCESSING.set(true);
            drainJobs();
        } finally {
            PROCESSING.set(false);
        }
    }

    private static void drainJobs() {
        int budget = ExcavateServerConfig.VEIN_BLOCKS_PER_TICK.get();
        var server = ServerLifecycleHooks.getCurrentServer();

        Iterator<Map.Entry<UUID, Deque<Job>>> playerIt = JOBS.entrySet().iterator();
        while (playerIt.hasNext()) {
            Map.Entry<UUID, Deque<Job>> entry = playerIt.next();
            Deque<Job> queue = entry.getValue();
            Job job = queue.peek();
            if (job == null) {
                playerIt.remove();
                continue;
            }

            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !(player.level() instanceof ServerLevel level)) {

                queue.clear();
                playerIt.remove();
                continue;
            }

            drainOneJob(player, level, job, budget);

            if (job.remaining.isEmpty()) {
                queue.poll();
                if (job.broken > 0) {
                    MinecraftForge.EVENT_BUS.post(new VeinMineEvent.Post(player, job.origin, job.broken));
                }
            }
            if (queue.isEmpty()) playerIt.remove();
        }
    }

    private static void drainOneJob(ServerPlayer player, ServerLevel level, Job job, int budget) {
        ItemStack tool = player.getMainHandItem();
        boolean bareHanded = tool.isEmpty();

        int processed = 0;
        while (processed < budget && !job.remaining.isEmpty()) {
            if (ExcavateServerConfig.effectiveRespectDurability() && !bareHanded && tool.isEmpty()) {
                job.remaining.clear();
                break;
            }

            BlockPos pos = job.remaining.poll();
            processed++;

            assert pos != null;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            if (!ClaimProtectionCompat.canBreak(player, pos)) continue;

            BlockEvent.BreakEvent extraEvent = new BlockEvent.BreakEvent(level, pos, state, player);
            MinecraftForge.EVENT_BUS.post(extraEvent);
            if (extraEvent.isCanceled()) continue;

            BlockEntity be = level.getBlockEntity(pos);

            ItemStack dropTool = ExcavateServerConfig.effectiveRespectEnchantments() ? tool : ItemStack.EMPTY;
            if (ExcavateServerConfig.effectiveCollectToPlayer()) {

                List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, dropTool);
                level.destroyBlock(pos, false, player);
                giveToPlayer(player, drops);
            } else {
                level.destroyBlock(pos, false, player);
                Block.dropResources(state, level, pos, be, player, dropTool);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.0);

            job.broken++;

            if (ExcavateServerConfig.effectiveRespectDurability() && !bareHanded) {
                tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                if (tool.isEmpty()) {
                    job.remaining.clear();
                    break;
                }
            }

            if (ExcavateServerConfig.effectiveRespectHunger()) {
                player.causeFoodExhaustion(0.005F);
            }
        }
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

}
