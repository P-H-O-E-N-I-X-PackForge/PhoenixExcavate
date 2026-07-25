package net.phoenixvine.excavate.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.vein.MatchModeRegistry;

import java.util.List;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VeinRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<BlockPos> preview = VeinPreviewTracker.getPreview();
        if (preview.size() <= 1) return;

        ExcavateThemePalette.refresh(ExcavateTheme.current());
        String override = ExcavateSettings.get().getOutlineColorHex();
        int accent = override.isBlank() ? ExcavateThemePalette.ACCENT :
                ExcavateUIKit.parseHexColor(override, ExcavateThemePalette.ACCENT);
        float r = ((accent >> 16) & 0xFF) / 255f;
        float g = ((accent >> 8) & 0xFF) / 255f;
        float b = (accent & 0xFF) / 255f;

        float pulse = 0.55f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 260.0);

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.5f);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());

        for (BlockPos pos : preview) {
            AABB box = new AABB(pos).inflate(0.002);
            LevelRenderer.renderLineBox(pose, vc, box, r, g, b, pulse);
        }

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        ExcavateHud.render(event.getGuiGraphics());

        List<BlockPos> preview = VeinPreviewTracker.getPreview();
        Minecraft mc = Minecraft.getInstance();
        String text;

        if (preview.isEmpty()) {
            return;
        } else if (preview.size() == 1) {

            MatchMode mode = MatchModeRegistry.byId(ExcavateSettings.get().getMatchModeId());
            String modeName = mode == null ? ExcavateSettings.get().getMatchModeId() :
                    net.minecraft.network.chat.Component.translatable(mode.translationKey()).getString();
            text = "§cVein: no match §7(mode: §f" + modeName + "§7)";
        } else {
            VeinShape shape = VeinClientState.getShape();
            String shapeName = net.minecraft.network.chat.Component.translatable(shape.translationKey()).getString();
            text = "§7Vein: §f" + (preview.size() - 1) + " §7— " + shapeName;
        }

        int screenW = event.getGuiGraphics().guiWidth();
        int screenH = event.getGuiGraphics().guiHeight();
        int x = screenW / 2 - mc.font.width(text) / 2;
        int y = screenH / 2 + 14;
        event.getGuiGraphics().drawString(mc.font, text, x, y, 0xFFFFFFFF, true);
    }
}
