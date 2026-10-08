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
import net.phoenixvine.wiki.PhoenixWikiAPI;
import net.phoenixvine.wiki.client.screen.WikiTheme;
import net.phoenixvine.wiki.theme.PhoenixTheme;
import net.phoenixvine.wiki.theme.PhoenixThemeEditorScreen;

import java.util.ArrayList;
import java.util.List;

public class ExcavateConfigScreen extends Screen {

    private static final int ROW_H = 18;

    private static final int MIN_PANEL_W = 480, MAX_PANEL_W = 620;
    private static final int MIN_PANEL_H = 380, MAX_PANEL_H = 420;
    private static final int[] SWATCHES = { 0xFFFFFF, 0x33CCFF, 0xFF4444, 0x44DD66, 0xFFCC33, 0xCC66FF, 0xFF8833 };

    private final Screen parent;
    private String activeList = "ore";
    private MatchEntry.Type newEntryType = MatchEntry.Type.BLOCK;
    private EditBox newEntryBox;
    private EditBox colorHexBox;
    private EditBox maxVeinSizeBox;

    private int panelX, panelY, panelW, panelH;

    private float uiScale = 1f;
    private int vw, vh;
    private int matchModeRowY, themeRowY, vsY;
    private int editThemeX, editThemeW, wikiX, wikiW, themeLinksY;
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

        float neededW = MIN_PANEL_W + 40f;
        float neededH = MIN_PANEL_H + 40f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        panelW = Math.min(MAX_PANEL_W, Math.max(MIN_PANEL_W, vw - 40));
        panelH = Math.min(MAX_PANEL_H, Math.max(MIN_PANEL_H, vh - 40));
        panelX = (vw - panelW) / 2;
        panelY = (vh - panelH) / 2;

        listX = panelX + panelW / 2 + 10;
        listY = panelY + 46;
        listW = panelX + panelW - 12 - listX;
        listH = panelH - 108;

        calculateLayout();

        newEntryBox = new EditBox(font, listX, panelY + panelH - 46, listW - 100, 16, Component.empty());
        newEntryBox.setHint(Component.literal("§8e.g. minecraft:diamond_ore"));
        newEntryBox.setMaxLength(256);
        addRenderableWidget(newEntryBox);

        int finalSwX = swatchRects.isEmpty() ? 0 : swatchRects.get(swatchRects.size() - 1)[0] + 16;
        colorHexBox = new EditBox(font, finalSwX + 4, themeLinksY + ROW_H + 3, 70, 14, Component.empty());
        colorHexBox.setHint(Component.literal("§8RRGGBB"));
        colorHexBox.setMaxLength(6);
        colorHexBox.setValue(ExcavateSettings.get().getOutlineColorHex());
        colorHexBox.setResponder(v -> {
            ExcavateSettings s = ExcavateSettings.get();
            s.setOutlineColorHex(v.trim());
            s.save();
        });
        addRenderableWidget(colorHexBox);

        int boxX = panelX + 12 + font.width("Max vein size: ") + 6;
        maxVeinSizeBox = new EditBox(font, boxX, vsY + 2, 45, 14, Component.empty());
        maxVeinSizeBox.setMaxLength(5);
        maxVeinSizeBox.setValue(String.valueOf(ExcavateSettings.get().getMaxVeinSize()));
        maxVeinSizeBox.setResponder(v -> {
            try {
                int val = Integer.parseInt(v.trim());
                ExcavateSettings s = ExcavateSettings.get();
                s.setMaxVeinSize(Math.max(1, Math.min(val, 4096)));
                s.save();
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(maxVeinSizeBox);
    }

    private void calculateLayout() {
        int leftX = panelX + 12;
        int ty = panelY + 28;

        themeRowY = ty;
        ty += ROW_H;

        themeLinksY = ty;
        editThemeX = leftX;
        editThemeW = font.width("✎ Edit colors...");
        wikiX = editThemeX + editThemeW + 14;
        wikiW = font.width("📖 Wiki");
        ty += ROW_H + 2;

        int swX = leftX + font.width("Outline color: ") + 2;
        int swY = ty + 2;
        swatchRects.clear();
        for (int c : SWATCHES) {
            swatchRects.add(new int[] { swX, swY, 12, 12 });
            swX += 15;
        }
        ty += ROW_H;

        resetColorX = leftX;
        resetColorY = ty;
        resetColorW = font.width("↺ Use theme color");
        ty += ROW_H + 4;

        matchModeRowY = ty;
        ty += ROW_H + 4;

        vsY = ty;
        ty += ROW_H + 6;

        toggleRowRects.clear();
        int leftColWidth = (panelW / 2) - 20;
        for (int i = 0; i < 10; i++) {
            toggleRowRects.add(new int[] { leftX, ty, leftColWidth, ROW_H });
            ty += ROW_H;
        }

        tabY = panelY + 28;
        tabW = (listW - 8 - RESET_LIST_BTN_W) / 2;
        oreTabX = listX;
        anyTabX = listX + tabW + 4;

        resetListX = anyTabX + tabW + 4;
        resetListY = tabY;
        resetListW = RESET_LIST_BTN_W;

        typeToggleY = panelY + panelH - 46;
        typeToggleW = 42;
        typeToggleX = listX + listW - typeToggleW - typeToggleW - 6;

        addBtnX = typeToggleX + typeToggleW + 4;
        addBtnY = typeToggleY;
        addBtnW = typeToggleW;
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float partial) {
        ExcavateThemePalette.refresh(PhoenixTheme.current());
        ExcavateSettings s = ExcavateSettings.get();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        ExcavateUIKit.drawModalChrome(g, font, vw, vh, panelX, panelY, panelW, panelH, 20, "§fPhoenix Ultimine Settings");
        g.drawString(font, "§8Esc or O to close", panelX + panelW - font.width("Esc or O to close") - 10,
                panelY + 7, ExcavateThemePalette.TEXT_FAINT, false);

        renderLeftColumn(g, mx, my, s);
        renderRightColumn(g, mx, my);
        super.render(g, mx, my, partial);

        g.pose().popPose();
    }

    private void renderLeftColumn(GuiGraphics g, int mx, int my, ExcavateSettings s) {
        int x = panelX + 12;
        int leftColWidth = (panelW / 2) - 20;

        boolean themeHov = hovRow(mx, my, x, themeRowY, leftColWidth);
        if (themeHov) g.fill(x - 2, themeRowY, x + leftColWidth, themeRowY + ROW_H, 0x22FFFFFF);
        g.drawString(font, "§7Theme: §f" + PhoenixTheme.getActiveName() + " §8(click)",
                x, themeRowY + 5, ExcavateThemePalette.TEXT, false);

        boolean editThemeHov = hovRow(mx, my, editThemeX, themeLinksY, editThemeW);
        g.drawString(font, editThemeHov ? "§b✎ Edit colors..." : "§8✎ Edit colors...",
                editThemeX, themeLinksY + 5, ExcavateThemePalette.TEXT_DIM, false);

        boolean wikiHov = hovRow(mx, my, wikiX, themeLinksY, wikiW);
        g.drawString(font, wikiHov ? "§b📖 Wiki" : "§8📖 Wiki", wikiX, themeLinksY + 5,
                ExcavateThemePalette.TEXT_DIM, false);

        g.drawString(font, "§7Outline color:", x, themeLinksY + ROW_H + 7, ExcavateThemePalette.TEXT, false);
        for (int i = 0; i < SWATCHES.length; i++) {
            int[] rect = swatchRects.get(i);
            boolean hov = mx >= rect[0] && mx < rect[0] + 12 && my >= rect[1] && my < rect[1] + 12;
            g.fill(rect[0], rect[1], rect[0] + 12, rect[1] + 12, 0xFF000000 | SWATCHES[i]);
            if (hov) ExcavateUIKit.drawBorder(g, rect[0] - 1, rect[1] - 1, 14, 14, 0xFFFFFFFF);
        }

        boolean resetHov = hovRow(mx, my, resetColorX, resetColorY, resetColorW);
        g.drawString(font, resetHov ? "§b↺ Theme color" : "§8↺ Theme color", resetColorX, resetColorY + 5,
                ExcavateThemePalette.TEXT_DIM, false);

        MatchMode matchMode = MatchModeRegistry.byId(s.getMatchModeId());
        String matchModeName = matchMode == null ? s.getMatchModeId() : Component.translatable(matchMode.translationKey()).getString();
        boolean matchModeHov = hovRow(mx, my, x, matchModeRowY, leftColWidth);
        if (matchModeHov) g.fill(x - 2, matchModeRowY, x + leftColWidth, matchModeRowY + ROW_H, 0x22FFFFFF);
        g.drawString(font, "§7Match: §f" + matchModeName + " §8(click)",
                x, matchModeRowY + 5, ExcavateThemePalette.TEXT, false);

        boolean vsLocked = ExcavateServerConfig.LOCK_MAX_VEIN_SIZE.get();
        g.drawString(font, "§7Max vein size:", x, vsY + 5, ExcavateThemePalette.TEXT, false);
        maxVeinSizeBox.setEditable(!vsLocked);
        if (vsLocked) {
            maxVeinSizeBox.setValue(String.valueOf(ExcavateServerConfig.effectiveMaxVeinSize()));
            g.drawString(font, "§6🔒", maxVeinSizeBox.getX() + 50, vsY + 5, ExcavateThemePalette.TEXT_FAINT, false);
        } else if (!maxVeinSizeBox.isFocused()) {
            maxVeinSizeBox.setValue(String.valueOf(s.getMaxVeinSize()));
        }

        toggleRowLocked.clear();
        drawToggleRow(g, 0, "Vein-mining hunger", ExcavateServerConfig.effectiveRespectHunger(), ExcavateServerConfig.LOCK_RESPECT_HUNGER.get(), mx, my);
        drawToggleRow(g, 1, "Gen. mining hunger", s.isGeneralMiningExhaustion(), false, mx, my);
        drawToggleRow(g, 2, "Respect durability", ExcavateServerConfig.effectiveRespectDurability(), ExcavateServerConfig.LOCK_RESPECT_DURABILITY.get(), mx, my);
        drawToggleRow(g, 3, "Respect enchants", ExcavateServerConfig.effectiveRespectEnchantments(), ExcavateServerConfig.LOCK_RESPECT_ENCHANTMENTS.get(), mx, my);
        drawToggleRow(g, 4, "Respect tool tier", s.isRespectToolTier(), false, mx, my);
        drawToggleRow(g, 5, "Hold to activate", s.isHoldToActivate(), false, mx, my);
        drawToggleRow(g, 6, "Include diagonals", s.isIncludeDiagonalNeighbors(), false, mx, my);
        drawToggleRow(g, 7, "Collect to inv.", ExcavateServerConfig.effectiveCollectToPlayer(), ExcavateServerConfig.LOCK_COLLECT_TO_PLAYER.get(), mx, my);
        drawToggleRow(g, 8, "Place consumes inv.", ExcavateServerConfig.effectivePlaceConsumesInventory(), ExcavateServerConfig.LOCK_PLACE_CONSUMES_INVENTORY.get(), mx, my);
        drawToggleRow(g, 9, "Place replaces match", ExcavateServerConfig.effectivePlaceReplacesMatching(), ExcavateServerConfig.LOCK_PLACE_REPLACES_MATCHING.get(), mx, my);
    }

    private void renderRightColumn(GuiGraphics g, int mx, int my) {
        boolean locked = MatchListConfig.isLocked(activeList);

        drawTabButton(g, oreTabX, tabY, tabW, "Ore list", activeList.equals("ore"), mx, my);
        drawTabButton(g, anyTabX, tabY, tabW, "Any list", activeList.equals("any"), mx, my);

        boolean resetListHov = !locked && hovRow(mx, my, resetListX, resetListY, resetListW);
        g.fill(resetListX, resetListY, resetListX + resetListW, resetListY + 16, resetListHov ? 0x33FFFFFF : ExcavateThemePalette.HEADER);
        ExcavateUIKit.drawBorder(g, resetListX, resetListY, resetListW, 16);
        String resetLabel = locked ? "§6🔒" : (resetListHov ? "§c↺ Rst" : "§7↺ Rst");
        g.drawCenteredString(font, resetLabel, resetListX + resetListW / 2, resetListY + 4, ExcavateThemePalette.TEXT_DIM);

        g.fill(listX, listY, listX + listW, listY + listH, ExcavateThemePalette.HEADER);
        ExcavateUIKit.drawBorder(g, listX, listY, listW, listH);

        listRowRects.clear();
        List<MatchEntry> entries = MatchListConfig.get(activeList);
        int ly = listY + 2;

        enableScissorScaled(g, listX - 4, listY, listX + listW, listY + listH);
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
            g.drawCenteredString(font, "§8(empty - add below)", listX + listW / 2, listY + listH / 2 - 4, ExcavateThemePalette.TEXT_FAINT);
        }

        newEntryBox.setEditable(!locked);
        if (locked) {
            g.drawString(font, "§6🔒 Locked by server", listX, panelY + panelH - 26, ExcavateThemePalette.TEXT_FAINT, false);
            return;
        }

        boolean typeHov = hovRow(mx, my, typeToggleX, typeToggleY, typeToggleW);
        g.fill(typeToggleX, typeToggleY, typeToggleX + typeToggleW, typeToggleY + 16, typeHov ? 0x33FFFFFF : ExcavateThemePalette.PANEL);
        ExcavateUIKit.drawBorder(g, typeToggleX, typeToggleY, typeToggleW, 16);
        String typeLabel = newEntryType == MatchEntry.Type.TAG ? "§bTag" : "§7Block";
        g.drawCenteredString(font, typeLabel, typeToggleX + typeToggleW / 2, typeToggleY + 4, ExcavateThemePalette.TEXT);

        boolean addHov = hovRow(mx, my, addBtnX, addBtnY, addBtnW);
        g.fill(addBtnX, addBtnY, addBtnX + addBtnW, addBtnY + 16, addHov ? 0x4466FF88 : 0x2266FF88);
        ExcavateUIKit.drawBorder(g, addBtnX, addBtnY, addBtnW, 16, 0xFF66FF88);
        g.drawCenteredString(font, "§a+ Add", addBtnX + addBtnW / 2, addBtnY + 4, 0xFFFFFFFF);
    }

    private void drawTabButton(GuiGraphics g, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hov = hovRow(mx, my, x, y, w);
        int bg = active ? (0x33 << 24) | (ExcavateThemePalette.ACCENT & 0x00FFFFFF) : (hov ? 0x22FFFFFF : ExcavateThemePalette.HEADER);
        g.fill(x, y, x + w, y + 16, bg);
        ExcavateUIKit.drawBorder(g, x, y, w, 16, active ? ExcavateThemePalette.ACCENT : ExcavateThemePalette.BORDER);
        g.drawCenteredString(font, (active ? "§f" : "§7") + label, x + w / 2, y + 4, active ? ExcavateThemePalette.TEXT : ExcavateThemePalette.TEXT_DIM);
    }

    private void enableScissorScaled(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(Math.round(x1 * uiScale), Math.round(y1 * uiScale), Math.round(x2 * uiScale),
                Math.round(y2 * uiScale));
    }

    private boolean hovRow(int mx, int my, int x, int y, int w) {
        return mx >= x && mx < x + w && my >= y && my < y + ROW_H;
    }

    private void drawToggleRow(GuiGraphics g, int index, String label, boolean value, boolean locked, int mx, int my) {
        int[] r = toggleRowRects.get(index);
        boolean hov = !locked && hovRow(mx, my, r[0], r[1], r[2]);
        if (hov) g.fill(r[0] - 2, r[1], r[0] + r[2], r[1] + ROW_H, 0x22FFFFFF);
        String mark = value ? "§a[x]" : "§8[ ]";
        String suffix = locked ? " §6🔒" : "";
        int color = locked ? ExcavateThemePalette.TEXT_FAINT : ExcavateThemePalette.TEXT;
        g.drawString(font, mark + " §7" + label + suffix, r[0], r[1] + 5, color, false);
        toggleRowLocked.add(locked);
    }

    @Override
    public boolean mouseClicked(double rawMx, double rawMy, int btn) {
        int mx = Math.round((float) (rawMx / uiScale));
        int my = Math.round((float) (rawMy / uiScale));
        ExcavateSettings s = ExcavateSettings.get();

        int leftColWidth = (panelW / 2) - 20;

        if (hovRow(mx, my, panelX + 12, themeRowY, leftColWidth)) {
            cycleTheme();
            return true;
        }

        if (hovRow(mx, my, editThemeX, themeLinksY, editThemeW)) {
            minecraft.setScreen(new PhoenixThemeEditorScreen(this, "Ultimine"));
            return true;
        }

        if (hovRow(mx, my, wikiX, themeLinksY, wikiW)) {
            openWiki();
            return true;
        }

        for (int i = 0; i < swatchRects.size(); i++) {
            int[] r = swatchRects.get(i);
            if (mx >= r[0] && mx < r[0] + 12 && my >= r[1] && my < r[1] + 12) {
                String hex = ExcavateUIKit.formatHexColor(SWATCHES[i]).substring(1);
                s.setOutlineColorHex(hex);
                colorHexBox.setValue(hex);
                s.save();
                return true;
            }
        }

        if (hovRow(mx, my, resetColorX, resetColorY, resetColorW)) {
            s.setOutlineColorHex("");
            colorHexBox.setValue("");
            s.save();
            return true;
        }

        if (hovRow(mx, my, panelX + 12, matchModeRowY, leftColWidth)) {
            MatchMode next = MatchModeRegistry.next(s.getMatchModeId());
            if (next != null) {
                s.setMatchModeId(next.id().toString());
                s.save();
                VeinClientState.notifyMatchModeChanged();
            }
            return true;
        }

        for (int i = 0; i < toggleRowRects.size(); i++) {
            int[] r = toggleRowRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                if (i < toggleRowLocked.size() && toggleRowLocked.get(i)) return true;

                switch (i) {
                    case 0 -> s.setRespectHunger(!s.isRespectHunger());
                    case 1 -> s.setGeneralMiningExhaustion(!s.isGeneralMiningExhaustion());
                    case 2 -> s.setRespectDurability(!s.isRespectDurability());
                    case 3 -> s.setRespectEnchantments(!s.isRespectEnchantments());
                    case 4 -> s.setRespectToolTier(!s.isRespectToolTier());
                    case 5 -> s.setHoldToActivate(!s.isHoldToActivate());
                    case 6 -> s.setIncludeDiagonalNeighbors(!s.isIncludeDiagonalNeighbors());
                    case 7 -> s.setCollectToPlayer(!s.isCollectToPlayer());
                    case 8 -> s.setPlaceConsumesInventory(!s.isPlaceConsumesInventory());
                    case 9 -> s.setPlaceReplacesMatching(!s.isPlaceReplacesMatching());
                }
                s.save();
                return true;
            }
        }

        if (hovRow(mx, my, oreTabX, tabY, tabW)) {
            activeList = "ore";
            return true;
        }
        if (hovRow(mx, my, anyTabX, tabY, tabW)) {
            activeList = "any";
            return true;
        }

        boolean listLocked = MatchListConfig.isLocked(activeList);
        if (!listLocked && hovRow(mx, my, resetListX, resetListY, resetListW)) {
            MatchListConfig.resetToDefaults(activeList);
            return true;
        }

        for (int i = 0; i < listRowRects.size(); i++) {
            int[] r = listRowRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                List<MatchEntry> entries = MatchListConfig.get(activeList);
                if (i < entries.size()) MatchListConfig.remove(activeList, entries.get(i));
                return true;
            }
        }

        if (!listLocked && mx >= typeToggleX && mx < typeToggleX + typeToggleW && my >= typeToggleY && my < typeToggleY + 16) {
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

    @Override
    public boolean mouseDragged(double rawMx, double rawMy, int btn, double dragX, double dragY) {
        return super.mouseDragged(rawMx / uiScale, rawMy / uiScale, btn, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rawMx, double rawMy, int btn) {
        return super.mouseReleased(rawMx / uiScale, rawMy / uiScale, btn);
    }

    @Override
    public boolean mouseScrolled(double rawMx, double rawMy, double delta) {
        double mx = rawMx / uiScale;
        double my = rawMy / uiScale;
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void cycleTheme() {
        List<String> names = new ArrayList<>(PhoenixTheme.REGISTRY.keySet());
        if (names.isEmpty()) return;
        int idx = names.indexOf(PhoenixTheme.getActiveName());
        PhoenixTheme.setCurrent(names.get((idx + 1) % names.size()));
    }

    private void openWiki() {
        if (minecraft == null) return;
        PhoenixTheme t = PhoenixTheme.current();
        WikiTheme wikiTheme = new WikiTheme(
                t.bg.getColor(), t.panel.getColor(), t.header.getColor(), t.border.getColor(),
                t.accent.getColor(), t.text.getColor(), t.textDim.getColor(), t.textFaint.getColor(),
                t.done.getColor(), t.activeColor.getColor());
        PhoenixWikiAPI.open(this, "phoenix_excavate", "wiki", wikiTheme);
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