package net.phoenixvine.excavate.client;

public class ExcavateThemePalette {

    public static int BG, PANEL, HEADER, BORDER, ACCENT;
    public static int TEXT, TEXT_DIM, TEXT_FAINT;

    public static void refresh(ExcavateTheme t) {
        BG = t.bg.getColor();
        PANEL = t.panel.getColor();
        HEADER = t.header.getColor();
        BORDER = t.border.getColor();
        ACCENT = t.accent.getColor();

        TEXT = t.text.getColor();
        TEXT_DIM = t.textDim.getColor();
        TEXT_FAINT = t.textFaint.getColor();
    }
}
