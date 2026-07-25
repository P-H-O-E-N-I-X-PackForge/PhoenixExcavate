package net.phoenixvine.excavate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchListConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Paths.get("config", "phoenix_excavate", "match_lists.json");

    private static Map<String, List<MatchEntry>> LISTS = new LinkedHashMap<>();

    private MatchListConfig() {}

    public static List<MatchEntry> get(String listName) {
        return LISTS.computeIfAbsent(listName, k -> new ArrayList<>());
    }

    public static boolean isLocked(String listName) {
        return ExcavateServerConfig.isListLocked(listName);
    }

    public static void add(String listName, MatchEntry entry) {
        if (isLocked(listName)) return;
        get(listName).add(entry);
        save();
    }

    public static void remove(String listName, MatchEntry entry) {
        if (isLocked(listName)) return;
        get(listName).remove(entry);
        save();
    }

    public static void resetToDefaults(String listName) {
        if (isLocked(listName)) return;
        List<MatchEntry> defaults = ExcavateServerConfig.defaultEntriesFor(listName);
        List<MatchEntry> list = get(listName);
        list.clear();
        list.addAll(defaults);
        save();
    }

    public static void load() {
        LISTS.clear();
        if (!Files.exists(FILE)) {
            seedDefaults();
            save();
            return;
        }
        try {
            String json = Files.readString(FILE);
            Type type = new TypeToken<Map<String, List<MatchEntry>>>() {}.getType();
            Map<String, List<MatchEntry>> parsed = GSON.fromJson(json, type);
            if (parsed != null) LISTS.putAll(parsed);
        } catch (Exception e) {
            e.printStackTrace();
            seedDefaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(LISTS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void seedDefaults() {
        LISTS.put("ore", ExcavateServerConfig.defaultEntriesFor("ore"));
        LISTS.put("any", ExcavateServerConfig.defaultEntriesFor("any"));
    }
}
