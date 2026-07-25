package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.phoenixvine.excavate.api.VeinShape;

public final class ExcavateHud {

    private static final long FADE_MS = 1400;

    private static String label = null;
    private static long shownAtMs = 0;

    private ExcavateHud() {}

    public static void showShapeToast(VeinShape shape) {
        label = Component.translatable(shape.translationKey()).getString();
        shownAtMs = System.currentTimeMillis();
    }

    public static void render(GuiGraphics g) {
        if (label == null) return;
        long elapsed = System.currentTimeMillis() - shownAtMs;
        if (elapsed > FADE_MS) {
            label = null;
            return;
        }

        ExcavateThemePalette.refresh(ExcavateTheme.current());
        Minecraft mc = Minecraft.getInstance();
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        float fadeT = elapsed < 200 ? elapsed / 200f : Math.max(0f, 1f - (elapsed - (FADE_MS - 300)) / 300f);
        int alpha = (int) (Math.max(0f, Math.min(1f, fadeT)) * 220);
        if (alpha <= 0) return;

        String text = "§7Vein Shape: §f" + label;
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
}
