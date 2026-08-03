package net.phoenixvine.excavate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExcavateSettings {

    private boolean respectHunger = true;

    private boolean generalMiningExhaustion = false;

    private boolean respectDurability = true;
    private boolean respectEnchantments = true;
    private boolean collectToPlayer = true;

    private int maxVeinSize = 64;
    private boolean includeDiagonalNeighbors = false;

    private boolean holdToActivate = true;

    private String matchModeId = "match_any";

    private String theme = "DARK";

    private String outlineColorHex = "";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_FILE = Paths.get("config", "phoenix_excavate_settings.json");

    private static ExcavateSettings INSTANCE = null;

    public static ExcavateSettings get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static ExcavateSettings load() {
        ExcavateSettings result;
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String json = Files.readString(SETTINGS_FILE);
                result = GSON.fromJson(json, ExcavateSettings.class);
                if (result == null) result = new ExcavateSettings();
            } else {
                result = new ExcavateSettings();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = new ExcavateSettings();
        }
        INSTANCE = result;
        return result;
    }

    public void save() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            Files.writeString(SETTINGS_FILE, GSON.toJson(this));
            INSTANCE = this;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRespectHunger() {
        return respectHunger;
    }

    public void setRespectHunger(boolean v) {
        respectHunger = v;
    }

    public boolean isGeneralMiningExhaustion() {
        return generalMiningExhaustion;
    }

    public void setGeneralMiningExhaustion(boolean v) {
        generalMiningExhaustion = v;
    }

    public String getMatchModeId() {
        return matchModeId == null || matchModeId.isBlank() ? "match_any" : matchModeId;
    }

    public void setMatchModeId(String v) {
        matchModeId = v;
    }

    public boolean isRespectDurability() {
        return respectDurability;
    }

    public void setRespectDurability(boolean v) {
        respectDurability = v;
    }

    public boolean isRespectEnchantments() {
        return respectEnchantments;
    }

    public void setRespectEnchantments(boolean v) {
        respectEnchantments = v;
    }

    public boolean isCollectToPlayer() {
        return collectToPlayer;
    }

    public void setCollectToPlayer(boolean v) {
        collectToPlayer = v;
    }

    public int getMaxVeinSize() {
        return maxVeinSize <= 0 ? 64 : Math.min(maxVeinSize, 4096);
    }

    public void setMaxVeinSize(int v) {
        maxVeinSize = Math.max(1, Math.min(4096, v));
    }

    public boolean isIncludeDiagonalNeighbors() {
        return includeDiagonalNeighbors;
    }

    public void setIncludeDiagonalNeighbors(boolean v) {
        includeDiagonalNeighbors = v;
    }

    public boolean isHoldToActivate() {
        return holdToActivate;
    }

    public void setHoldToActivate(boolean v) {
        holdToActivate = v;
    }

    public String getTheme() {
        return theme == null ? "DARK" : theme;
    }

    public void setTheme(String t) {
        theme = t;
    }

    public String getOutlineColorHex() {
        return outlineColorHex == null ? "" : outlineColorHex;
    }

    public void setOutlineColorHex(String hex) {
        outlineColorHex = hex == null ? "" : hex;
    }
}
