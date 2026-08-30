package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.vein.FacingUtil;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinFinder;

import java.util.List;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VeinPreviewTracker {

    private static List<BlockPos> preview = List.of();
    private static BlockPos lastOrigin = null;
    private static Direction lastFacing = null;
    private static String lastShapeId = null;
    private static String lastMatchModeId = null;
    private static boolean lastActive = false;

    public static List<BlockPos> getPreview() {
        return preview;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean active = VeinClientState.isActive();

        if (!active) {
            if (lastActive) clear();
            return;
        }

        BlockPos origin = lookedAtBlock(mc);
        String shapeId = VeinClientState.getShapeId();
        String matchModeId = ExcavateSettings.get().getMatchModeId();
        Direction facing = mc.player == null ? Direction.NORTH :
                FacingUtil.facingOf(mc.player.getLookAngle(), lastFacing);

        if (origin == null) {
            if (lastOrigin != null) clear();
            return;
        }

        if (origin.equals(lastOrigin) && facing == lastFacing && shapeId.equals(lastShapeId) &&
                matchModeId.equals(lastMatchModeId) && lastActive) {
            return;
        }

        lastOrigin = origin;
        lastFacing = facing;
        lastShapeId = shapeId;
        lastMatchModeId = matchModeId;
        lastActive = true;

        VeinShape shape = VeinClientState.getShape();
        MatchMode matchMode = MatchModeRegistry.byId(matchModeId);
        preview = mc.level == null || mc.player == null ? List.of()
                : VeinFinder.find(mc.level, origin, facing, matchMode, shape, mc.player.getMainHandItem());
    }

    private static void clear() {
        preview = List.of();
        lastOrigin = null;
        lastFacing = null;
        lastShapeId = null;
        lastMatchModeId = null;
        lastActive = false;
    }

    private static BlockPos lookedAtBlock(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) hit).getBlockPos();
    }
}
