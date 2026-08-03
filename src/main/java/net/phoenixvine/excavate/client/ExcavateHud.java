package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.VeinShape;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExcavateHud {

    private static final long FADE_MS = 1400;

    private static String label = null;
    private static long shownAtMs = 0;

    private static final String[] SUITE_ORDER = {
        "solaris", "phoenix_essentials", "phoenix_domains", "phoenix_chronicles", "phoenix_guilds", "phoenix_excavate"
    };
    private static final String SELF_ID = "phoenix_excavate";

    public static final int BTN_SIZE = 20;
    public static final int GAP = 2;
    public static final int MARGIN = 4;

    private static final int GRID_COLUMNS = 3;

    private static final ResourceLocation ICON_TEXTURE =
        new ResourceLocation(PhoenixExcavate.MOD_ID, "textures/gui/suite_bar_icon.png");

    private static int suiteIconCountFor(String modId) {
        if (!modId.equals("phoenix_essentials")) return 1;
        Minecraft mc = Minecraft.getInstance();
        return (mc.player != null && mc.player.hasPermissions(2)) ? 5 : 2;
    }

    private static int suiteSlotIndex(String selfId) {
        int idx = 0;
        for (String id : SUITE_ORDER) {
            if (id.equals(selfId)) return idx;
            if (net.minecraftforge.fml.ModList.get().isLoaded(id)) idx += suiteIconCountFor(id);
        }
        return idx;
    }

    public static int suiteButtonX() {
        int col = suiteSlotIndex(SELF_ID) % GRID_COLUMNS;
        return MARGIN + col * (BTN_SIZE + GAP);
    }

    public static int suiteButtonY() {
        int row = suiteSlotIndex(SELF_ID) / GRID_COLUMNS;
        return MARGIN + row * (BTN_SIZE + GAP);
    }

    public static boolean isPointInSuiteButton(double guiX, double guiY) {
        int x = suiteButtonX();
        int y = suiteButtonY();
        return guiX >= x && guiX < x + BTN_SIZE && guiY >= y && guiY < y + BTN_SIZE;
    }

    private ExcavateHud() {}

    public static void showShapeToast(VeinShape shape) {
        label = Component.translatable(shape.translationKey()).getString();
        shownAtMs = System.currentTimeMillis();
    }

    public static void render(GuiGraphics g) {
        ExcavateThemePalette.refresh(ExcavateTheme.current());

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

    private static void drawSuiteButton(GuiGraphics g, double mouseX, double mouseY, boolean hovered) {
        Minecraft mc = Minecraft.getInstance();
        int x = suiteButtonX();
        int y = suiteButtonY();

        int panelColor = hovered ? ExcavateThemePalette.ACCENT : ExcavateThemePalette.PANEL;
        g.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, panelColor);
        ExcavateUIKit.drawBorder(g, x, y, BTN_SIZE, BTN_SIZE, ExcavateThemePalette.ACCENT);

        g.blit(ICON_TEXTURE, x + 2, y + 2, 0, 0, 16, 16, 16, 16);

        if (hovered) {
            g.renderTooltip(mc.font, Component.literal("§fOpen Excavate Settings"), (int) mouseX, (int) mouseY);
        }
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

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ExcavateSuiteBarClickHandler.screenWantsBar(event.getScreen())) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        boolean hovered = isPointInSuiteButton(mouseX, mouseY);
        drawSuiteButton(event.getGuiGraphics(), mouseX, mouseY, hovered);
    }
}
