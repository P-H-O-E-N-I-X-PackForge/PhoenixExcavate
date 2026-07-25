package net.phoenixvine.excavate.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ExcavateServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_DURABILITY;
    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_HUNGER;
    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_ENCHANTMENTS;
    public static final ForgeConfigSpec.IntValue MAX_VEIN_SIZE_CAP;
    public static final ForgeConfigSpec.BooleanValue LOCK_MAX_VEIN_SIZE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_SHAPES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_MATCH_MODES;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ORE_LIST_DEFAULTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANY_LIST_DEFAULTS;
    public static final ForgeConfigSpec.BooleanValue LOCK_ORE_LIST;
    public static final ForgeConfigSpec.BooleanValue LOCK_ANY_LIST;

    public static final ForgeConfigSpec.IntValue VEIN_BLOCKS_PER_TICK;
    public static final ForgeConfigSpec.IntValue VEIN_TRIGGER_DEBOUNCE_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("policy");

        LOCK_RESPECT_DURABILITY = builder
                .comment("If true, players cannot disable durability cost - it is always respected regardless of their own setting.")
                .define("lockRespectDurability", false);

        LOCK_RESPECT_HUNGER = builder
                .comment("If true, players cannot disable vein-mining's hunger cost.")
                .define("lockRespectHunger", false);

        LOCK_RESPECT_ENCHANTMENTS = builder
                .comment("If true, players cannot disable Fortune/Silk Touch being respected on vein-mined drops.")
                .define("lockRespectEnchantments", false);

        MAX_VEIN_SIZE_CAP = builder
                .comment("Hard ceiling on how many blocks a single vein-mine can break, regardless of any player's own max-vein-size preference.")
                .defineInRange("maxVeinSizeCap", 256, 1, 4096);

        LOCK_MAX_VEIN_SIZE = builder
                .comment("If true, players cannot raise (or lower) their own max vein size at all - it is fixed " +
                        "to maxVeinSizeCap for everyone, with no per-player adjustment. Useful if a pack considers " +
                        "even letting players opt into a big vein size too easy/game-breaking on its own.")
                .define("lockMaxVeinSize", false);

        DISABLED_SHAPES = builder
                .comment("Vein-mining shape ids players may not select at all (e.g. \"tunnel\"). Built-ins: blob, tunnel, layer, wall, staircase, shapeless.")
                .defineList("disabledShapes", new ArrayList<String>(), o -> o instanceof String);

        DISABLED_MATCH_MODES = builder
                .comment("Match-mode ids players may not select at all (e.g. \"match_any\"). Built-ins: exact, match_ore, match_any.")
                .defineList("disabledMatchModes", new ArrayList<String>(), o -> o instanceof String);

        builder.pop();
        builder.push("matchLists");

        ORE_LIST_DEFAULTS = builder
                .comment("Default entries for the first (\"ore\") match list - both its initial contents on first " +
                        "launch and what the in-game Reset button restores it to. Each entry is \"TAG:<id>\" or " +
                        "\"BLOCK:<id>\" (e.g. \"TAG:minecraft:iron_ores\"). Repoint this at anything you like - it " +
                        "doesn't have to actually be ores.")
                .defineList("oreListDefaults", defaultOreEntries(), o -> o instanceof String);

        ANY_LIST_DEFAULTS = builder
                .comment("Default entries for the second (\"any\") match list, used by Match Any List mode. " +
                        "Unlike the ore list, this ISN'T a whitelist - Match Any List already matches every block " +
                        "by default (stone, dirt, untagged modded blocks, everything). Entries here only matter " +
                        "for blocks that are ALSO in the ore list: those are excluded from Any mode unless also " +
                        "listed here, letting ores stay reserved for Match Ore List mode if desired. Default " +
                        "re-includes the ore tags plus logs/leaves. Same \"TAG:<id>\"/\"BLOCK:<id>\" entry format " +
                        "as oreListDefaults.")
                .defineList("anyListDefaults", defaultAnyEntries(), o -> o instanceof String);

        LOCK_ORE_LIST = builder
                .comment("If true, players cannot add, remove, or reset entries in the ore list at all - it is " +
                        "fixed to oreListDefaults.")
                .define("lockOreList", false);

        LOCK_ANY_LIST = builder
                .comment("If true, players cannot add, remove, or reset entries in the any list at all - it is " +
                        "fixed to anyListDefaults.")
                .define("lockAnyList", false);

        builder.pop();
        builder.push("performance");

        VEIN_BLOCKS_PER_TICK = builder
                .comment("How many extra blocks a single vein-mine may destroy per server tick. Breaking a whole " +
                        "large vein in one tick is what causes the noticeable stutter/hitch big vein-mines are " +
                        "known for (the same complaint FTB's own vein miner gets) - spreading it across several " +
                        "ticks instead keeps each tick's cost small. Lower this further if you still see hitching " +
                        "on a slow server; raise it if veins feel too slow to finish.")
                .defineInRange("veinBlocksPerTick", 8, 1, 4096);

        VEIN_TRIGGER_DEBOUNCE_TICKS = builder
                .comment("Minimum number of ticks that must pass between one vein-mine trigger and the next, per " +
                        "player. Without this, another mod's own multi-block AOE mining tool (e.g. a GregTech " +
                        "drill) breaking several blocks in a single action fires one genuine block-break event per " +
                        "block - and each of those could independently satisfy the vein-mine trigger, spawning " +
                        "its own full vein job on top of the others and compounding into a runaway break radius. " +
                        "This debounce is far shorter than any legitimate gap between a player's own deliberate " +
                        "vein-mine actions, so it doesn't affect normal play.")
                .defineInRange("veinTriggerDebounceTicks", 5, 0, 200);

        builder.pop();
        SPEC = builder.build();
    }

    private static List<String> defaultOreEntries() {
        List<String> defaults = new ArrayList<>();
        defaults.add("TAG:minecraft:coal_ores");
        defaults.add("TAG:minecraft:iron_ores");
        defaults.add("TAG:minecraft:copper_ores");
        defaults.add("TAG:minecraft:gold_ores");
        defaults.add("TAG:minecraft:redstone_ores");
        defaults.add("TAG:minecraft:lapis_ores");
        defaults.add("TAG:minecraft:diamond_ores");
        defaults.add("TAG:minecraft:emerald_ores");
        return defaults;
    }

    private static List<String> defaultAnyEntries() {

        List<String> defaults = defaultOreEntries();
        defaults.add("TAG:minecraft:logs");
        defaults.add("TAG:minecraft:leaves");
        return defaults;
    }

    private ExcavateServerConfig() {}

    public static boolean effectiveRespectDurability() {
        return LOCK_RESPECT_DURABILITY.get() || ExcavateSettings.get().isRespectDurability();
    }

    public static boolean effectiveRespectHunger() {
        return LOCK_RESPECT_HUNGER.get() || ExcavateSettings.get().isRespectHunger();
    }

    public static boolean effectiveRespectEnchantments() {
        return LOCK_RESPECT_ENCHANTMENTS.get() || ExcavateSettings.get().isRespectEnchantments();
    }

    public static int effectiveMaxVeinSize() {
        if (LOCK_MAX_VEIN_SIZE.get()) return MAX_VEIN_SIZE_CAP.get();
        return Math.min(ExcavateSettings.get().getMaxVeinSize(), MAX_VEIN_SIZE_CAP.get());
    }

    public static boolean isShapeAllowed(String shapeId) {
        return !DISABLED_SHAPES.get().contains(shapeId);
    }

    public static boolean isMatchModeAllowed(String matchModeId) {
        return !DISABLED_MATCH_MODES.get().contains(matchModeId);
    }

    public static boolean isListLocked(String listName) {
        if ("ore".equals(listName)) return LOCK_ORE_LIST.get();
        if ("any".equals(listName)) return LOCK_ANY_LIST.get();
        return false;
    }

    public static List<MatchEntry> defaultEntriesFor(String listName) {

        List<? extends String> raw;
        if (!SPEC.isLoaded()) {
            raw = "ore".equals(listName) ? defaultOreEntries() : "any".equals(listName) ? defaultAnyEntries() : List.of();
        } else {
            raw = "ore".equals(listName) ? ORE_LIST_DEFAULTS.get() :
                    "any".equals(listName) ? ANY_LIST_DEFAULTS.get() : List.of();
        }
        List<MatchEntry> result = new ArrayList<>();
        for (String s : raw) {
            MatchEntry entry = MatchEntry.parse(s);
            if (entry != null) result.add(entry);
        }
        return result;
    }
}
