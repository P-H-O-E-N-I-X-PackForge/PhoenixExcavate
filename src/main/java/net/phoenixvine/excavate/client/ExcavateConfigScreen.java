package net.phoenixvine.excavate.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.config.MatchEntry;
import net.phoenixvine.excavate.config.MatchListConfig;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.vein.MatchModeRegistry;

import java.util.ArrayList;
import java.util.List;

public class ExcavateConfigScreen extends Screen {

    private static final int ROW_H = 18;
    private static final int MIN_PANEL_W = 320, MAX_PANEL_W = 600;
    private static final int MIN_PANEL_H = 250, MAX_PANEL_H = 340;
    private static final int[] SWATCHES = { 0xFFFFFF, 0x33CCFF, 0xFF4444, 0x44DD66, 0xFFCC33, 0xCC66FF, 0xFF8833 };

    private final Screen parent;
    private String activeList = "ore";
    private MatchEntry.Type newEntryType = MatchEntry.Type.BLOCK;
    private EditBox newEntryBox;
    private EditBox colorHexBox;

    private float uiScale = 1f;
    private int panelX, panelY, panelW, panelH;
    private int matchModeRowY, themeRowY, vsMinusX, vsPlusX, vsY;
    private int typeToggleX, typeToggleY, typeToggleW;
    private int addBtnX, addBtnY, addBtnW;
    private static final int RESET_LIST_BTN_W = 44;
    private int oreTabX, anyTabX, tabY, tabW;
    private int resetListX, resetListY, resetListW;
    private int listX, listY, listW, listH;
    private int resetColorX, resetColorY, resetColorW;
    private final List<int[]> swatchRects = new ArrayList<>();

    private final List<int[]> toggleRowRects = new ArrayList<>();
    private final List<Boolean> toggleRowLocked = new ArrayList<>();
    private final List<int[]> listRowRects = new ArrayList<>();

    public ExcavateConfigScreen(Screen parent) {
        super(Component.literal("Phoenix Ultimine"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        uiScale = Math.min(1f, Math.min((width - 16f) / MIN_PANEL_W, (height - 16f) / MIN_PANEL_H));
        uiScale = Math.max(0.5f, uiScale);
        int vw = Math.round(width / uiScale);
        int vh = Math.round(height / uiScale);

        int desiredW = Math.max(MIN_PANEL_W, Math.min(MAX_PANEL_W, vw - 40));
        int desiredH = Math.max(MIN_PANEL_H, Math.min(MAX_PANEL_H, vh - 60));
        panelW = Math.min(desiredW, Math.max(160, vw - 16));
        panelH = Math.min(desiredH, Math.max(140, vh - 16));
        panelX = (vw - panelW) / 2;
        panelY = (vh - panelH) / 2;

        listX = panelX + panelW / 2 + 6;
        listY = panelY + 46;
        listW = panelX + panelW - 12 - listX;
        listH = panelH - 108;

        newEntryBox = new EditBox(font, listX, panelY + panelH - 46, listW - 96, 16, Component.empty());
        newEntryBox.setHint(Component.literal("§8e.g. minecraft:diamond_ore"));
        newEntryBox.setMaxLength(256);
        addRenderableWidget(newEntryBox);

        colorHexBox = new EditBox(font, 0, 0, 70, 14, Component.empty());
        colorHexBox.setHint(Component.literal("§8RRGGBB"));
        colorHexBox.setMaxLength(6);
        colorHexBox.setValue(ExcavateSettings.get().getOutlineColorHex());
        colorHexBox.setResponder(v -> {
            ExcavateSettings s = ExcavateSettings.get();
            s.setOutlineColorHex(v.trim());
            s.save();
        });
        addRenderableWidget(colorHexBox);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        ExcavateThemePalette.refresh(ExcavateTheme.current());
        ExcavateSettings s = ExcavateSettings.get();

        int vmx = Math.round(mx / uiScale);
        int vmy = Math.round(my / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        int vw = Math.round(width / uiScale);
        int vh = Math.round(height / uiScale);
        ExcavateUIKit.drawModalChrome(g, font, vw, vh, panelX, panelY, panelW, panelH, 20,
                "§fPhoenix Ultimine Settings");
        g.drawString(font, "§8Esc or O to close", panelX + panelW - font.width("Esc or O to close") - 10,
                panelY + 7, ExcavateThemePalette.TEXT_FAINT, false);

        renderLeftColumn(g, vmx, vmy, s);
        renderRightColumn(g, vmx, vmy);

        super.render(g, vmx, vmy, partial);

        g.pose().popPose();
    }

    private void renderLeftColumn(GuiGraphics g, int mx, int my, ExcavateSettings s) {
        int x = panelX + 12;
        int ty = panelY + 28;

        themeRowY = ty;
        boolean themeHov = hovRow(mx, my, x, ty, 220);
        if (themeHov) g.fill(x - 2, ty, x + 220, ty + ROW_H, 0x22FFFFFF);
        g.drawString(font, "§7Theme: §f" + ExcavateTheme.getActiveName() + " §8(click to cycle)",
                x, ty + 5, ExcavateThemePalette.TEXT, false);
        ty += ROW_H + 2;

        g.drawString(font, "§7Outline color:", x, ty + 5, ExcavateThemePalette.TEXT, false);
        int swX = x + font.width("Outline color: ") + 2;
        int swY = ty + 2;
        swatchRects.clear();
        for (int c : SWATCHES) {
            boolean hov = mx >= swX && mx < swX + 12 && my >= swY && my < swY + 12;
            g.fill(swX, swY, swX + 12, swY + 12, 0xFF000000 | c);
            if (hov) ExcavateUIKit.drawBorder(g, swX - 1, swY - 1, 14, 14, 0xFFFFFFFF);
            swatchRects.add(new int[] { swX, swY, 12, 12 });
            swX += 15;
        }
        colorHexBox.setX(swX + 4);
        colorHexBox.setY(swY - 1);
        ty += ROW_H;
        resetColorX = x;
        resetColorY = ty;
        resetColorW = font.width("↺ Use theme color");
        boolean resetHov = hovRow(mx, my, resetColorX, resetColorY, resetColorW);
        g.drawString(font, resetHov ? "§b↺ Use theme color" : "§8↺ Use theme color", resetColorX, resetColorY + 5,
                ExcavateThemePalette.TEXT_DIM, false);
        ty += ROW_H + 4;

        MatchMode matchMode = MatchModeRegistry.byId(s.getMatchModeId());
        String matchModeName = matchMode == null ? s.getMatchModeId() :
                Component.translatable(matchMode.translationKey()).getString();
        matchModeRowY = ty;
        boolean matchModeHov = hovRow(mx, my, x, ty, 220);
        if (matchModeHov) g.fill(x - 2, ty, x + 220, ty + ROW_H, 0x22FFFFFF);
        g.drawString(font, "§7Match mode: §f" + matchModeName + " §8(click to cycle)",
                x, ty + 5, ExcavateThemePalette.TEXT, false);
        ty += ROW_H + 4;

        vsY = ty;
        boolean vsLocked = ExcavateServerConfig.LOCK_MAX_VEIN_SIZE.get();
        int vsShown = vsLocked ? ExcavateServerConfig.effectiveMaxVeinSize() : s.getMaxVeinSize();
        String vsLabel = "§7Max vein size: §f" + vsShown;
        g.drawString(font, vsLabel, x, ty + 5, ExcavateThemePalette.TEXT, false);
        vsMinusX = x + font.width(vsLabel) + 10;
        vsPlusX = vsMinusX + 16;
        if (vsLocked) {
            g.drawString(font, "§6🔒", vsMinusX, ty + 5, ExcavateThemePalette.TEXT_FAINT, false);
        } else {
            drawStepperButton(g, vsMinusX, ty, "-", mx, my);
            drawStepperButton(g, vsPlusX, ty, "+", mx, my);
        }
        ty += ROW_H + 6;

        toggleRowRects.clear();
        toggleRowLocked.clear();
        ty = drawToggleRow(g, x, ty, "Vein-mining hunger cost",
                ExcavateServerConfig.effectiveRespectHunger(), ExcavateServerConfig.LOCK_RESPECT_HUNGER.get(), mx, my);
        ty = drawToggleRow(g, x, ty, "General mining hunger cost", s.isGeneralMiningExhaustion(), false, mx, my);
        ty = drawToggleRow(g, x, ty, "Respect durability",
                ExcavateServerConfig.effectiveRespectDurability(), ExcavateServerConfig.LOCK_RESPECT_DURABILITY.get(),
                mx, my);
        ty = drawToggleRow(g, x, ty, "Respect enchantments",
                ExcavateServerConfig.effectiveRespectEnchantments(),
                ExcavateServerConfig.LOCK_RESPECT_ENCHANTMENTS.get(), mx, my);
        ty = drawToggleRow(g, x, ty, "Hold to activate (off = toggle)", s.isHoldToActivate(), false, mx, my);
        drawToggleRow(g, x, ty, "Include diagonal neighbors", s.isIncludeDiagonalNeighbors(), false, mx, my);
    }

    private void renderRightColumn(GuiGraphics g, int mx, int my) {
        boolean locked = MatchListConfig.isLocked(activeList);

        tabY = panelY + 28;
        tabW = (listW - 4 - 4 - RESET_LIST_BTN_W) / 2;
        oreTabX = listX;
        anyTabX = listX + tabW + 4;
        drawTabButton(g, oreTabX, tabY, tabW, "Ore list", activeList.equals("ore"), mx, my);
        drawTabButton(g, anyTabX, tabY, tabW, "Any list", activeList.equals("any"), mx, my);

        resetListX = anyTabX + tabW + 4;
        resetListY = tabY;
        resetListW = RESET_LIST_BTN_W;
        boolean resetListHov = !locked && hovRow(mx, my, resetListX, resetListY, resetListW);
        g.fill(resetListX, resetListY, resetListX + resetListW, resetListY + 16,
                resetListHov ? 0x33FFFFFF : ExcavateThemePalette.HEADER);
        ExcavateUIKit.drawBorder(g, resetListX, resetListY, resetListW, 16);
        String resetLabel = locked ? "§6🔒" : (resetListHov ? "§c↺ Reset" : "§7↺ Reset");
        g.drawCenteredString(font, resetLabel, resetListX + resetListW / 2, resetListY + 4,
                ExcavateThemePalette.TEXT_DIM);

        g.fill(listX, listY, listX + listW, listY + listH, ExcavateThemePalette.HEADER);
        ExcavateUIKit.drawBorder(g, listX, listY, listW, listH);

        listRowRects.clear();
        List<MatchEntry> entries = MatchListConfig.get(activeList);
        int ly = listY + 2;
        g.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < entries.size() && ly < listY + listH; i++) {
            MatchEntry e = entries.get(i);
            boolean hov = !locked && mx >= listX && mx < listX + listW && my >= ly && my < ly + ROW_H;
            if (hov) g.fill(listX, ly, listX + listW, ly + ROW_H, 0x22FFFFFF);
            String tag = e.type == MatchEntry.Type.TAG ? "§b#" : "§7";
            g.drawString(font, tag + e.id, listX + 4, ly + 5, ExcavateThemePalette.TEXT_DIM, false);
            if (!locked) {
                g.drawString(font, "§c✕", listX + listW - 14, ly + 5, 0xFFFF5555, false);
                listRowRects.add(new int[] { listX, ly, listW, ROW_H });
            }
            ly += ROW_H;
        }
        g.disableScissor();
        if (entries.isEmpty()) {
            g.drawCenteredString(font, "§8(empty - add one below)", listX + listW / 2, listY + listH / 2 - 4,
                    ExcavateThemePalette.TEXT_FAINT);
        }

        newEntryBox.setEditable(!locked);
        if (locked) {
            g.drawString(font, "§6🔒 Locked by server - contents fixed by the pack/server owner", listX,
                    panelY + panelH - 26, ExcavateThemePalette.TEXT_FAINT, false);
            return;
        }

        typeToggleY = panelY + panelH - 46;
        typeToggleW = 42;
        typeToggleX = listX + listW - typeToggleW - typeToggleW - 6;
        boolean typeHov = hovRow(mx, my, typeToggleX, typeToggleY, typeToggleW);
        g.fill(typeToggleX, typeToggleY, typeToggleX + typeToggleW, typeToggleY + 16,
                typeHov ? 0x33FFFFFF : ExcavateThemePalette.PANEL);
        ExcavateUIKit.drawBorder(g, typeToggleX, typeToggleY, typeToggleW, 16);
        String typeLabel = newEntryType == MatchEntry.Type.TAG ? "§bTag" : "§7Block";
        g.drawCenteredString(font, typeLabel, typeToggleX + typeToggleW / 2, typeToggleY + 4,
                ExcavateThemePalette.TEXT);

        addBtnX = typeToggleX + typeToggleW + 4;
        addBtnY = typeToggleY;
        addBtnW = typeToggleW;
        boolean addHov = hovRow(mx, my, addBtnX, addBtnY, addBtnW);
        g.fill(addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + 16, addHov ? 0x4466FF88 : 0x2266FF88);
        ExcavateUIKit.drawBorder(g, addBtnX, addBtnY, addBtnW, 16, 0xFF66FF88);
        g.drawCenteredString(font, "§a+ Add", addBtnX + addBtnW / 2, addBtnY + 4, 0xFFFFFFFF);

        g.drawString(font, "§8Click Block/Tag to switch entry type", listX, panelY + panelH - 26,
                ExcavateThemePalette.TEXT_FAINT, false);
    }

    private void drawTabButton(GuiGraphics g, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hov = hovRow(mx, my, x, y, w);
        int bg = active ? (0x33 << 24) | (ExcavateThemePalette.ACCENT & 0x00FFFFFF) :
                (hov ? 0x22FFFFFF : ExcavateThemePalette.HEADER);
        g.fill(x, y, x + w, y + 16, bg);
        ExcavateUIKit.drawBorder(g, x, y, w, 16, active ? ExcavateThemePalette.ACCENT : ExcavateThemePalette.BORDER);
        g.drawCenteredString(font, (active ? "§f" : "§7") + label, x + w / 2, y + 4,
                active ? ExcavateThemePalette.TEXT : ExcavateThemePalette.TEXT_DIM);
    }

    private void drawStepperButton(GuiGraphics g, int x, int y, String symbol, int mx, int my) {
        boolean hov = hovRow(mx, my, x, y, 14);
        g.fill(x, y, x + 14, y + 14, hov ? 0x33FFFFFF : ExcavateThemePalette.PANEL);
        ExcavateUIKit.drawBorder(g, x, y, 14, 14);
        g.drawCenteredString(font, "§f" + symbol, x + 7, y + 3, ExcavateThemePalette.TEXT);
    }

    private boolean hovRow(int mx, int my, int x, int y, int w) {
        return mx >= x && mx < x + w && my >= y && my < y + ROW_H;
    }

    private int drawToggleRow(GuiGraphics g, int x, int y, String label, boolean value, boolean locked, int mx,
                              int my) {
        boolean hov = !locked && hovRow(mx, my, x, y, 240);
        if (hov) g.fill(x - 2, y, x + 240, y + ROW_H, 0x22FFFFFF);
        String mark = value ? "§a[x]" : "§8[ ]";
        String suffix = locked ? " §6🔒" : "";
        int color = locked ? ExcavateThemePalette.TEXT_FAINT : ExcavateThemePalette.TEXT;
        g.drawString(font, mark + " §7" + label + suffix, x, y + 5, color, false);
        toggleRowRects.add(new int[] { x, y, 240, ROW_H });
        toggleRowLocked.add(locked);
        return y + ROW_H;
    }

    @Override
    public boolean mouseClicked(double rawMx, double rawMy, int btn) {
        double mx = rawMx / uiScale;
        double my = rawMy / uiScale;
        ExcavateSettings s = ExcavateSettings.get();

        if (hovRow((int) mx, (int) my, panelX + 12, themeRowY, 220)) {
            cycleTheme();
            return true;
        }

        for (int[] r : swatchRects) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                int idx = swatchRects.indexOf(r);
                String hex = ExcavateUIKit.formatHexColor(SWATCHES[idx]).substring(1);
                s.setOutlineColorHex(hex);
                colorHexBox.setValue(hex);
                s.save();
                return true;
            }
        }

        if (hovRow((int) mx, (int) my, resetColorX, resetColorY, resetColorW)) {
            s.setOutlineColorHex("");
            colorHexBox.setValue("");
            s.save();
            return true;
        }

        if (hovRow((int) mx, (int) my, panelX + 12, matchModeRowY, 220)) {
            MatchMode next = MatchModeRegistry.next(s.getMatchModeId());
            if (next != null) {
                s.setMatchModeId(next.id());
                s.save();
                VeinClientState.notifyMatchModeChanged();
            }
            return true;
        }

        if (!ExcavateServerConfig.LOCK_MAX_VEIN_SIZE.get()) {
            if (mx >= vsMinusX && mx < vsMinusX + 14 && my >= vsY && my < vsY + 14) {
                s.setMaxVeinSize(s.getMaxVeinSize() - 4);
                s.save();
                return true;
            }
            if (mx >= vsPlusX && mx < vsPlusX + 14 && my >= vsY && my < vsY + 14) {
                s.setMaxVeinSize(s.getMaxVeinSize() + 4);
                s.save();
                return true;
            }
        }

        for (int i = 0; i < toggleRowRects.size(); i++) {
            int[] r = toggleRowRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                if (toggleRowLocked.get(i)) return true;
                switch (i) {
                    case 0 -> s.setRespectHunger(!s.isRespectHunger());
                    case 1 -> s.setGeneralMiningExhaustion(!s.isGeneralMiningExhaustion());
                    case 2 -> s.setRespectDurability(!s.isRespectDurability());
                    case 3 -> s.setRespectEnchantments(!s.isRespectEnchantments());
                    case 4 -> s.setHoldToActivate(!s.isHoldToActivate());
                    case 5 -> s.setIncludeDiagonalNeighbors(!s.isIncludeDiagonalNeighbors());
                    default -> {}
                }
                s.save();
                return true;
            }
        }

        if (hovRow((int) mx, (int) my, oreTabX, tabY, tabW)) {
            activeList = "ore";
            return true;
        }
        if (hovRow((int) mx, (int) my, anyTabX, tabY, tabW)) {
            activeList = "any";
            return true;
        }

        boolean listLocked = MatchListConfig.isLocked(activeList);

        if (!listLocked && hovRow((int) mx, (int) my, resetListX, resetListY, resetListW)) {
            MatchListConfig.resetToDefaults(activeList);
            return true;
        }

        for (int i = 0; i < listRowRects.size(); i++) {
            int[] r = listRowRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                List<MatchEntry> entries = MatchListConfig.get(activeList);
                if (i < entries.size()) {
                    MatchListConfig.remove(activeList, entries.get(i));
                }
                return true;
            }
        }

        if (!listLocked && mx >= typeToggleX && mx < typeToggleX + typeToggleW && my >= typeToggleY &&
                my < typeToggleY + 16) {
            newEntryType = newEntryType == MatchEntry.Type.BLOCK ? MatchEntry.Type.TAG : MatchEntry.Type.BLOCK;
            return true;
        }

        if (!listLocked && mx >= addBtnX && mx < addBtnX + addBtnW && my >= addBtnY && my < addBtnY + 16) {
            String text = newEntryBox.getValue().trim();
            if (!text.isEmpty()) {
                MatchListConfig.add(activeList, new MatchEntry(newEntryType, text));
                newEntryBox.setValue("");
            }
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void cycleTheme() {
        List<String> names = new ArrayList<>(ExcavateTheme.REGISTRY.keySet());
        if (names.isEmpty()) return;
        int idx = names.indexOf(ExcavateTheme.getActiveName());
        String next = names.get((idx + 1) % names.size());
        ExcavateTheme.setCurrent(next);
    }

    @Override
    public boolean mouseScrolled(double rawMx, double rawMy, double delta) {
        double mx = rawMx / uiScale;
        double my = rawMy / uiScale;
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            return true;
        }
        if (ExcavateServerConfig.LOCK_MAX_VEIN_SIZE.get()) return true;
        ExcavateSettings s = ExcavateSettings.get();
        s.setMaxVeinSize(s.getMaxVeinSize() + (int) Math.signum(delta) * 4);
        s.save();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int mods) {
        if (ExcavateKeyBindings.OPEN_SETTINGS.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, mods);
    }

    @Override
    public void onClose() {
        MatchListConfig.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
