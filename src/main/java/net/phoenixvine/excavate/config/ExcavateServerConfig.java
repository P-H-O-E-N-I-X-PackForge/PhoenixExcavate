package net.phoenixvine.excavate.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenixvine.excavate.PhoenixExcavate;

import java.util.ArrayList;
import java.util.List;

public class ExcavateServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_DURABILITY;
    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_HUNGER;
    public static final ForgeConfigSpec.BooleanValue LOCK_RESPECT_ENCHANTMENTS;
    public static final ForgeConfigSpec.BooleanValue LOCK_COLLECT_TO_PLAYER;
    public static final ForgeConfigSpec.BooleanValue LOCK_TOOL_TIER;
    public static final ForgeConfigSpec.BooleanValue LOCK_PLACE_CONSUMES_INVENTORY;
    public static final ForgeConfigSpec.BooleanValue LOCK_PLACE_REPLACES_MATCHING;
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

    public static final ForgeConfigSpec.ConfigValue<String> REQUIRED_ENCHANTMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("policy");

        LOCK_RESPECT_DURABILITY = builder
                .comment("If true, players cannot disable durability cost. It is always respected regardless of their own setting.")
                .define("lockRespectDurability", false);

        LOCK_RESPECT_HUNGER = builder
                .comment("If true, players cannot disable vein-mining's hunger cost.")
                .define("lockRespectHunger", false);

        LOCK_RESPECT_ENCHANTMENTS = builder
                .comment("If true, players cannot disable Fortune/Silk Touch being respected on vein-mined drops.")
                .define("lockRespectEnchantments", false);

        LOCK_COLLECT_TO_PLAYER = builder
                .comment("If true, players cannot disable vein-mined drops being " +
                        "collected directly into their " +
                        "inventory. It is always on regardless of their own setting.")
                .define("lockCollectToPlayer", false);

        LOCK_TOOL_TIER = builder
                .comment("If true, players cannot disable vein-mined ores respecting tool tier." +
                        "It is always on regardless of their own setting.")
                .define("lockToolTier", false);

        LOCK_PLACE_CONSUMES_INVENTORY = builder
                .comment("If true, players cannot disable vein-placing consuming one matching item per block " +
                        "placed. It always costs inventory regardless of their own setting (creative-mode " +
                        "players are never charged either way).")
                .define("lockPlaceConsumesInventory", false);

        LOCK_PLACE_REPLACES_MATCHING = builder
                .comment("If true, players cannot disable vein-placing overwriting existing blocks that match " +
                        "the active match mode (e.g. re-skinning a wall). It always can, regardless of their " +
                        "own setting. Overwritten blocks are always given back to the player either way; " +
                        "bedrock (and anything else unbreakable) can never be overwritten regardless of this.")
                .define("lockPlaceReplacesMatching", false);

        LOCK_MAX_VEIN_SIZE = builder
                .comment("If true, players cannot raise (or lower) their own max vein size at all. It is fixed " +
                        "to maxVeinSizeCap for everyone, with no per-player adjustment. Useful if a pack considers " +
                        "even letting players opt into a big vein size too easy/game-breaking on its own.")
                .define("lockMaxVeinSize", false);

        MAX_VEIN_SIZE_CAP = builder
                .comment("Hard ceiling on how many blocks a single vein-mine can break, regardless of any player's own max-vein-size preference.")
                .defineInRange("maxVeinSizeCap", 256, 1, 16096);

        DISABLED_SHAPES = builder
                .comment("Vein-mining shape ids players may not select at all (e.g. \"tunnel\"). Built-ins: blob, tunnel, layer, wall, staircase, shapeless.")
                .defineList("disabledShapes", new ArrayList<>(), o -> o instanceof String);

        DISABLED_MATCH_MODES = builder
                .comment("Match-mode ids players may not select at all (e.g. \"match_any\"). Built-ins: exact, match_ore, match_any.")
                .defineList("disabledMatchModes", new ArrayList<>(), o -> o instanceof String);

        builder.pop();
        builder.push("matchLists");

        ORE_LIST_DEFAULTS = builder
                .comment("Default entries for the first (\"ore\") match list. Both its initial contents on first " +
                        "launch and what the in-game Reset button restores it to. Each entry is \"TAG:<id>\" or " +
                        "\"BLOCK:<id>\" (e.g. \"TAG:forge:ores/iron\"). Uses Forge's common ore tag convention " +
                        "(forge:ores/<name>) rather than vanilla's per-variant-family tags, so modded ores that " +
                        "register into the Forge common tags are recognized too. Repoint this at anything you " +
                        "like. It doesn't have to actually be ores.")
                .defineList("oreListDefaults", defaultOreEntries(), o -> o instanceof String);

        ANY_LIST_DEFAULTS = builder
                .comment("Default entries for the second (\"any\") match list, used by Match Any List mode. " +
                        "Unlike the ore list, this ISN'T a whitelist. Match Any List already matches every block " +
                        "by default (stone, dirt, untagged modded blocks, everything). Entries here only matter " +
                        "for blocks that are ALSO in the ore list: those are excluded from Any mode unless also " +
                        "listed here, letting ores stay reserved for Match Ore List mode if desired. Default " +
                        "re-includes the ore tags plus logs/leaves. Same \"TAG:<id>\"/\"BLOCK:<id>\" entry format " +
                        "as oreListDefaults.")
                .defineList("anyListDefaults", defaultAnyEntries(), o -> o instanceof String);

        LOCK_ORE_LIST = builder
                .comment("If true, players cannot add, remove, or reset entries in the ore list at all. It is " +
                        "fixed to oreListDefaults.")
                .define("lockOreList", false);

        LOCK_ANY_LIST = builder
                .comment("If true, players cannot add, remove, or reset entries in the any list at all. It is " +
                        "fixed to anyListDefaults.")
                .define("lockAnyList", false);

        builder.pop();
        builder.push("performance");

        VEIN_BLOCKS_PER_TICK = builder
                .comment("How many extra blocks a single vein-mine may destroy per server tick. Breaking a whole " +
                        "large vein in one tick is what causes the noticeable stutter/hitch big vein-mines are " +
                        "known for. Spreading it across several " +
                        "ticks instead keeps each tick's cost small. Lower this further if you still see hitching " +
                        "on a slow server; raise it if veins feel too slow to finish.")
                .defineInRange("veinBlocksPerTick", 16, 1, 16096);

        VEIN_TRIGGER_DEBOUNCE_TICKS = builder
                .comment("Minimum number of ticks that must pass between one vein-mine trigger and the next, per " +
                        "player. Without this, another mod's own multi-block AOE mining tool (e.g. a GregTech " +
                        "drill) breaking several blocks in a single action fires one genuine block-break event per " +
                        "block. And each of those could independently satisfy the vein-mine trigger, spawning " +
                        "its own full vein job on top of the others and compounding into a runaway break radius. " +
                        "This debounce is far shorter than any legitimate gap between a player's own deliberate " +
                        "vein-mine actions, so it doesn't affect normal play.")
                .defineInRange("veinTriggerDebounceTicks", 5, 0, 200);

        builder.pop();
        builder.push("requirements");

        REQUIRED_ENCHANTMENT = builder
                .comment("If set, vein-mining only triggers when the player's held tool carries this enchantment " +
                        "(registry id, e.g. \"minecraft:silk_touch\", or a modded enchant's id). Leave blank " +
                        "(default) to require nothing. This only gates whether a break continues into a full " +
                        "vein-mine. It doesn't touch vanilla mining of the single block itself. For anything more " +
                        "elaborate than \"needs one specific enchant\" (a custom item, an NBT flag, a whole other " +
                        "mod's mechanic), use ExcavateAPI.registerItemGate(...) instead. This config option is " +
                        "just the no-code path for the common case.")
                .define("requiredEnchantment", "");

        builder.pop();
        SPEC = builder.build();
    }

    private static volatile boolean warnedBadEnchantId = false;
    private static volatile boolean warnedUnknownEnchant = false;

    public static boolean hasRequiredEnchantment(ItemStack tool) {
        String id = REQUIRED_ENCHANTMENT.get().trim();
        if (id.isEmpty()) return true;

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            if (!warnedBadEnchantId) {
                warnedBadEnchantId = true;
                PhoenixExcavate.LOGGER.warn(
                        "requiredEnchantment '{}' is not a valid registry id - ignoring it (vein-mining is NOT " +
                                "gated). Expected format like \"minecraft:silk_touch\".",
                        id);
            }
            return true;
        }

        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(rl);
        if (enchantment == null) {
            if (!warnedUnknownEnchant) {
                warnedUnknownEnchant = true;
                PhoenixExcavate.LOGGER.warn(
                        "requiredEnchantment '{}' does not match any registered enchantment (mod not installed, " +
                                "or a typo?) - ignoring it (vein-mining is NOT gated).",
                        id);
            }
            return true;
        }

        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, tool) > 0;
    }

    private static List<String> defaultOreEntries() {
        List<String> defaults = new ArrayList<>();

        defaults.add("TAG:forge:ores");
        defaults.add("TAG:forge:ores/coal");
        defaults.add("TAG:forge:ores/iron");
        defaults.add("TAG:forge:ores/copper");
        defaults.add("TAG:forge:ores/gold");
        defaults.add("TAG:forge:ores/redstone");
        defaults.add("TAG:forge:ores/lapis");
        defaults.add("TAG:forge:ores/diamond");
        defaults.add("TAG:forge:ores/emerald");
        defaults.add("TAG:forge:ores/quartz");
        defaults.add("TAG:forge:ores/netherite_scrap");
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

    public static boolean effectiveCollectToPlayer() {
        return LOCK_COLLECT_TO_PLAYER.get() || ExcavateSettings.get().isCollectToPlayer();
    }

    public static boolean effectivePlaceConsumesInventory() {
        return LOCK_PLACE_CONSUMES_INVENTORY.get() || ExcavateSettings.get().isPlaceConsumesInventory();
    }

    public static boolean effectivePlaceReplacesMatching() {
        return LOCK_PLACE_REPLACES_MATCHING.get() || ExcavateSettings.get().isPlaceReplacesMatching();
    }

    public static int effectiveMaxVeinSize() {
        if (LOCK_MAX_VEIN_SIZE.get()) return MAX_VEIN_SIZE_CAP.get();
        return Math.min(ExcavateSettings.get().getMaxVeinSize(), MAX_VEIN_SIZE_CAP.get());
    }

    private static String stripNamespace(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    public static boolean isShapeAllowed(String shapeId) {
        return !DISABLED_SHAPES.get().contains(stripNamespace(shapeId));
    }

    public static boolean isMatchModeAllowed(String matchModeId) {
        return !DISABLED_MATCH_MODES.get().contains(stripNamespace(matchModeId));
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
