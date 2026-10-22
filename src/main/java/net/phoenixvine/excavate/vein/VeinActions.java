package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinAction;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.vein.action.HarvestAction;
import net.phoenixvine.excavate.vein.action.PlantAction;
import net.phoenixvine.excavate.vein.action.ReplaceAction;
import net.phoenixvine.excavate.vein.action.ToolUseAction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public final class VeinActions {

    private VeinActions() {}

    private static final class Job {
        final VeinAction action;
        final VeinAction.Context context;
        final Deque<BlockPos> remaining;

        Job(VeinAction action, VeinAction.Context context, List<BlockPos> targets) {
            this.action = action;
            this.context = context;
            this.remaining = new ArrayDeque<>(targets);
        }
    }

    private static final Map<UUID, Deque<Job>> JOBS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    public static void registerBuiltins() {
        ExcavateAPI.registerAction(new HarvestAction());
        ExcavateAPI.registerAction(new ToolUseAction());
        ExcavateAPI.registerAction(new ReplaceAction());
        ExcavateAPI.registerAction(new PlantAction());
    }

    public static boolean onBreak(ServerPlayer player, ServerLevel level, BlockPos origin,
                                  VeinServerState.Active active) {
        return run(VeinAction.Trigger.BREAK, player, level, origin, Direction.UP, active);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        if (active == null) return;

        try {
            Direction face = event.getFace() == null ? Direction.UP : event.getFace();
            if (run(VeinAction.Trigger.USE, player, level, event.getPos(), face, active)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } catch (Throwable t) {
            PhoenixExcavate.LOGGER.error("Vein action failed for {} at {}: the click itself is unaffected.",
                    player.getName().getString(), event.getPos(), t);
        }
    }

    private static boolean run(VeinAction.Trigger trigger, ServerPlayer player, ServerLevel level, BlockPos origin,
                               Direction face, VeinServerState.Active active) {
        VeinAction.Context context = new VeinAction.Context(player, level, origin, level.getBlockState(origin),
                player.getMainHandItem().copy(), face, active.mode());

        for (VeinAction action : ExcavateAPI.getActions()) {
            if (action.trigger() != trigger) continue;
            if (!action.appliesTo(context)) continue;
            if (action.featureId() != null && !ExcavateAPI.isFeatureEnabled(action.featureId(),
                    level.dimension().location(), player, context.held())) {
                continue;
            }

            var config = VeinUtil.getValidatedConfig(active);
            if (config == null || debounced(player, level)) return false;

            ItemStack finderTool = new ItemStack(Items.NETHERITE_PICKAXE);
            List<BlockPos> vein = new ArrayList<>(VeinFinder.find(level, origin,
                    FacingUtil.facingOf(player.getLookAngle()), config.matchMode(), config.shape(), finderTool));

            if (action.sameBlockOnly()) {
                vein.removeIf(pos -> level.getBlockState(pos).getBlock() != context.originState().getBlock());
            }
            vein.remove(origin);

            boolean handledOrigin = false;
            if (action.includesOrigin()) handledOrigin = action.applyTo(context, origin);
            if (!vein.isEmpty()) {
                JOBS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).add(new Job(action, context, vein));
            }
            return action.includesOrigin() && handledOrigin;
        }
        return false;
    }

    private static boolean debounced(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        Long last = LAST_TRIGGER_TICK.get(player.getUUID());
        if (last != null && now - last < ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get()) return true;
        LAST_TRIGGER_TICK.put(player.getUUID(), now);
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;

        int budget = ExcavateServerConfig.VEIN_BLOCKS_PER_TICK.get();
        var server = ServerLifecycleHooks.getCurrentServer();

        JOBS.entrySet().removeIf(entry -> {
            Deque<Job> queue = entry.getValue();
            Job job = queue.peek();
            if (job == null) return true;

            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(entry.getKey());
            // fake players (other mods, tests) are not in the player list, but are fine to keep working for
            if (player == null && !job.context.player().isRemoved()) player = job.context.player();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                queue.clear();
                return true;
            }

            int processed = 0;
            while (processed < budget && !job.remaining.isEmpty()) {
                if (!job.action.canContinue(job.context)) {
                    job.remaining.clear();
                    break;
                }
                BlockPos pos = job.remaining.poll();
                processed++;
                try {
                    job.action.applyTo(job.context, pos);
                } catch (Throwable t) {
                    PhoenixExcavate.LOGGER.error("Vein action {} failed at {}", job.action.id(), pos, t);
                }
            }

            if (job.remaining.isEmpty()) queue.poll();
            return queue.isEmpty();
        });
    }
}
