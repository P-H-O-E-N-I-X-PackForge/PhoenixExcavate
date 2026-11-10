package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.util.FakePlayer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public class VeinBreaker {

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    private static final class Job {
        final BlockPos origin;
        final Deque<BlockPos> remaining;
        /** Set for fake players (machines), which can't be looked up in the player list. Null for real players. */
        final ServerPlayer actor;
        int broken = 0;

        Job(BlockPos origin, List<BlockPos> candidates, ServerPlayer actor) {
            this.origin = origin;
            this.remaining = new ArrayDeque<>(candidates);
            this.actor = actor;
        }
    }

    private static final Map<UUID, Deque<Job>> JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || Boolean.TRUE.equals(PROCESSING.get())) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        // Create deployers and friends break blocks as a FakePlayer. Create even reports the deployer's OWNER's uuid
        // from getUUID(), so without this check a deployer would pick up its owner's vein-mine toggle and then
        // drain the vein through the real owner (their tool, hunger and inventory, from wherever they are).
        if (player instanceof FakePlayer && !ExcavateServerConfig.ALLOW_FAKE_PLAYERS.get()) return;

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

        boolean machine = player instanceof FakePlayer;

        if (!machine && settings.isGeneralMiningExhaustion()) {
            player.causeFoodExhaustion(0.005F);
        }

        VeinServerState.Active active = machine ? machineActive() : VeinServerState.active(player.getUUID());
        if (active == null) return;
        if (VeinActions.onBreak(player, level, event.getPos(), active)) {
            event.setCanceled(true);
            return;
        }
        if (active.mode() != VeinMode.MINE) return;

        ItemStack heldTool = player.getMainHandItem();

        if (!ExcavateAPI.isFeatureEnabled(ExcavateAPI.FEATURE_VEIN_MINING, level.dimension().location(), player,
                heldTool)) {
            return;
        }

        if (!ExcavateServerConfig.hasRequiredEnchantment(heldTool)) {
            return;
        }

        String shapeId = active.shapeId().toString();
        String matchModeId = active.matchModeId().toString();

        if (!ExcavateServerConfig.isShapeAllowed(shapeId) ||
                !ExcavateServerConfig.isMatchModeAllowed(matchModeId)) {
            return;
        }

        var config = VeinUtil.getValidatedConfig(active);
        if (config == null) return;

        VeinShape shape = config.shape();
        MatchMode matchMode = config.matchMode();

        long now = level.getGameTime();
        UUID key = keyOf(player);
        Long lastTrigger = LAST_TRIGGER_TICK.get(key);
        int debounceTicks = ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get();
        if (lastTrigger != null && now - lastTrigger < debounceTicks) return;
        LAST_TRIGGER_TICK.put(key, now);
        if (LAST_TRIGGER_TICK.size() > 256) {
            // Machines get a key per fake player instance, so forget the ones that have gone quiet.
            LAST_TRIGGER_TICK.values().removeIf(t -> now - t > 1200L);
        }

        BlockPos origin = event.getPos();
        Direction facing = FacingUtil.facingOf(player.getLookAngle());
        List<BlockPos> vein = VeinFinder.find(level, origin, facing, matchMode, shape, heldTool);
        if (vein.size() <= 1) return;

        List<BlockPos> extras = new ArrayList<>(vein.subList(1, vein.size()));

        VeinMineEvent.Pre pre = new VeinMineEvent.Pre(player, origin, extras);
        MinecraftForge.EVENT_BUS.post(pre);
        if (pre.isCanceled() || pre.getCandidates().isEmpty()) return;

        BlockState originState = level.getBlockState(origin);
        SoundType soundType = originState.getSoundType();
        level.playSound(null, origin, soundType.getBreakSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

        JOBS.computeIfAbsent(key, k -> new ArrayDeque<>())
                .add(new Job(origin, pre.getCandidates(), machine ? player : null));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;

        try {
            PROCESSING.set(true);
            drainJobs();
        } finally {
            PROCESSING.set(false);
        }
    }

    @SuppressWarnings("Duplicates")
    private static void drainJobs() {
        int budget = ExcavateServerConfig.VEIN_BLOCKS_PER_TICK.get();
        var server = ServerLifecycleHooks.getCurrentServer();

        JOBS.entrySet().removeIf(entry -> {
            Deque<Job> queue = entry.getValue();
            Job job = queue.peek();
            if (job == null) return true;

            ServerPlayer player = job.actor != null ? job.actor
                    : (server == null ? null : server.getPlayerList().getPlayer(entry.getKey()));
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                queue.clear();
                return true;
            }

            try {
                drainOneJob(player, level, job, budget);
            } catch (Throwable t) {
                // A block that throws while being broken must not take the server tick down with it: drop this job.
                PhoenixExcavate.LOGGER.error("Vein-mine job at {} failed and was dropped.", job.origin, t);
                job.remaining.clear();
            }

            if (job.remaining.isEmpty()) {
                queue.poll();
                if (job.broken > 0) {
                    MinecraftForge.EVENT_BUS.post(new VeinMineEvent.Post(player, job.origin, job.broken));
                }
            }

            return queue.isEmpty();
        });
    }

    private static void drainOneJob(ServerPlayer player, ServerLevel level, Job job, int budget) {
        ItemStack tool = player.getMainHandItem();
        boolean bareHanded = tool.isEmpty();

        boolean respectDurability = ExcavateServerConfig.effectiveRespectDurability() && !bareHanded;
        boolean respectEnchantments = ExcavateServerConfig.effectiveRespectEnchantments();
        // Machines have no inventory to collect into and no hunger bar: drops fall in the world instead.
        boolean machine = player instanceof FakePlayer;
        boolean collectToPlayer = !machine && ExcavateServerConfig.effectiveCollectToPlayer();
        boolean respectHunger = !machine && ExcavateServerConfig.effectiveRespectHunger();

        int processed = 0;
        while (processed < budget && !job.remaining.isEmpty()) {
            if (respectDurability && tool.isEmpty()) {
                job.remaining.clear();
                break;
            }

            BlockPos pos = job.remaining.poll();
            processed++;

            if (pos == null) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            if (!ClaimProtectionCompat.canBreak(player, pos)) continue;

            BlockEvent.BreakEvent extraEvent = new BlockEvent.BreakEvent(level, pos, state, player);
            MinecraftForge.EVENT_BUS.post(extraEvent);
            if (extraEvent.isCanceled()) continue;

            BlockEntity be = level.getBlockEntity(pos);
            ItemStack dropTool = respectEnchantments ? tool : ItemStack.EMPTY;

            if (collectToPlayer) {
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

            if (respectDurability) {

                tool.mineBlock(level, state, pos, player);
                if (tool.isEmpty()) {
                    job.remaining.clear();
                    break;
                }
            }

            if (respectHunger) {
                player.causeFoodExhaustion(0.005F);
            }
        }
    }

    /** Debounce / job key. Fake players get a key per instance, never their owner's uuid. */
    private static UUID keyOf(ServerPlayer player) {
        if (player instanceof FakePlayer) {
            return new UUID(0x46414B45504C5952L, System.identityHashCode(player));
        }
        return player.getUUID();
    }

    private static VeinServerState.Active machineActive() {
        ResourceLocation shape = ResourceLocation.tryParse(ExcavateServerConfig.MACHINE_SHAPE.get().trim());
        ResourceLocation mode = ResourceLocation.tryParse(ExcavateServerConfig.MACHINE_MATCH_MODE.get().trim());
        if (shape == null || mode == null) return null;
        return new VeinServerState.Active(shape, mode, VeinMode.MINE);
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
