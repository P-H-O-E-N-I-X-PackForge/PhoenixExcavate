package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.vein.VeinMode;
import net.phoenixvine.wiki.theme.PhoenixTheme;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExcavateHud {

    private static final long FADE_MS = 1400;

    private static String label = null;
    private static long shownAtMs = 0;

    private ExcavateHud() {}

    public static void showShapeToast(VeinShape shape) {
        label = "§7Vein Shape: §f" + Component.translatable(shape.translationKey()).getString();
        shownAtMs = System.currentTimeMillis();
    }

    public static void showModeToast(VeinMode mode) {
        label = "§7Vein Mode: §f" + (mode == VeinMode.PLACE ? "Place" : "Mine");
        shownAtMs = System.currentTimeMillis();
    }

    public static void render(GuiGraphics g) {
        ExcavateThemePalette.refresh(PhoenixTheme.current());

        if (label == null) return;
        long elapsed = System.currentTimeMillis() - shownAtMs;
        if (elapsed > FADE_MS) {
            label = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        float fadeT = elapsed < 200 ? elapsed / 200f : Math.max(0f, 1f - (elapsed - (FADE_MS - 300)) / 300f);
        int alpha = (int) (Math.max(0f, Math.min(1f, fadeT)) * 220);
        if (alpha <= 0) return;

        String text = label;
        int textW = mc.font.width(text);
        int boxW = textW + 16;
        int boxH = 20;
        int x = screenW / 2 - boxW / 2;
        int y = screenH / 2 + 40;

        int panelColor = (alpha << 24) | (ExcavateThemePalette.PANEL & 0x00FFFFFF);
        int borderColor = (alpha << 24) | (ExcavateThemePalette.ACCENT & 0x00FFFFFF);
        int textColor = (Math.min(255, alpha + 40) << 24) | (ExcavateThemePalette.TEXT & 0x00FFFFFF);

        g.fill(x, y, x + boxW, y + boxH, panelColor);
        ExcavateUIKit.drawBorder(g, x, y, boxW, boxH, borderColor);
        g.drawCenteredString(mc.font, text, screenW / 2, y + (boxH - mc.font.lineHeight) / 2, textColor);
    }

    private static void drawPickaxeGlyph(GuiGraphics g, int x, int y, int color) {

        for (int i = 0; i < 9; i++) {
            g.fill(x + 6 + i, y + 13 - i, x + 7 + i, y + 14 - i, color);
        }

        g.fill(x + 4, y + 5, x + 10, y + 6, color);
        g.fill(x + 5, y + 6, x + 9, y + 7, color);
        g.fill(x + 10, y + 5, x + 15, y + 6, color);
        g.fill(x + 10, y + 6, x + 14, y + 7, color);
    }
}
