package net.phoenixvine.excavate.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.wiki.api.gate.SuiteGates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The server settings that can differ from player to player, as gates open.
 * <p>
 * {@link ExcavateServerConfig} holds the base values. {@code config/phoenix_excavate_gates.json} lists <b>tiers</b>:
 * each has a {@code when} (a {@link SuiteGates} expression such as {@code conflux:node/phoenixcore:foo | solaris:x}), an
 * optional {@code scope} (a dimension id, or {@code *}, the default) and a {@code set} of any of the settings below,
 * named as in the server config. A player's settings are the base values with every tier whose expression is open
 * applied in the order they are written, later tiers winning. So a pack lists its tiers from the lowest up.
 * <p>
 * {@code { "when": "conflux:node/phoenixcore:excavate_2", "set": { "maxVeinSizeCap": 128, "lockMaxVeinSize": true } }}
 */
public final class ExcavatePolicy {

    /** Everything a tier can change. A record so two of them can be compared to see whether to send an update. */
    public record Resolved(int maxVeinSizeCap, boolean lockMaxVeinSize, boolean lockRespectDurability,
                           boolean lockRespectHunger, boolean lockRespectEnchantments, boolean lockCollectToPlayer,
                           boolean lockToolTier, boolean lockPlaceConsumesInventory, boolean lockPlaceReplacesMatching,
                           boolean lockOreList, boolean lockAnyList, List<String> disabledShapes,
                           List<String> disabledMatchModes, boolean harvestReplants, List<String> useExtraItems,
                           int veinBlocksPerTick, int veinTriggerDebounceTicks, String requiredEnchantment,
                           boolean allowFakePlayers, String machineShape, String machineMatchMode,
                           List<String> oreListDefaults, List<String> anyListDefaults) {

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(maxVeinSizeCap);
            buf.writeBoolean(lockMaxVeinSize);
            buf.writeBoolean(lockRespectDurability);
            buf.writeBoolean(lockRespectHunger);
            buf.writeBoolean(lockRespectEnchantments);
            buf.writeBoolean(lockCollectToPlayer);
            buf.writeBoolean(lockToolTier);
            buf.writeBoolean(lockPlaceConsumesInventory);
            buf.writeBoolean(lockPlaceReplacesMatching);
            buf.writeBoolean(lockOreList);
            buf.writeBoolean(lockAnyList);
            buf.writeCollection(disabledShapes, FriendlyByteBuf::writeUtf);
            buf.writeCollection(disabledMatchModes, FriendlyByteBuf::writeUtf);
            buf.writeBoolean(harvestReplants);
            buf.writeCollection(useExtraItems, FriendlyByteBuf::writeUtf);
            buf.writeVarInt(veinBlocksPerTick);
            buf.writeVarInt(veinTriggerDebounceTicks);
            buf.writeUtf(requiredEnchantment);
            buf.writeBoolean(allowFakePlayers);
            buf.writeUtf(machineShape);
            buf.writeUtf(machineMatchMode);
            buf.writeCollection(oreListDefaults, FriendlyByteBuf::writeUtf);
            buf.writeCollection(anyListDefaults, FriendlyByteBuf::writeUtf);
        }

        public static Resolved read(FriendlyByteBuf buf) {
            return new Resolved(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(), buf.readList(FriendlyByteBuf::readUtf),
                    buf.readList(FriendlyByteBuf::readUtf), buf.readBoolean(), buf.readList(FriendlyByteBuf::readUtf),
                    buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readBoolean(), buf.readUtf(),
                    buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf));
        }
    }

    /** The settings being built while tiers are applied. */
    private static final class Mut {

        int maxVeinSizeCap;
        boolean lockMaxVeinSize, lockRespectDurability, lockRespectHunger, lockRespectEnchantments,
                lockCollectToPlayer, lockToolTier, lockPlaceConsumesInventory, lockPlaceReplacesMatching,
                lockOreList, lockAnyList, harvestReplants;
        List<String> disabledShapes, disabledMatchModes, useExtraItems;
        int veinBlocksPerTick, veinTriggerDebounceTicks;
        String requiredEnchantment;
        boolean allowFakePlayers;
        String machineShape, machineMatchMode;
        List<String> oreListDefaults, anyListDefaults;

        Mut() {
            maxVeinSizeCap = ExcavateServerConfig.MAX_VEIN_SIZE_CAP.get();
            lockMaxVeinSize = ExcavateServerConfig.LOCK_MAX_VEIN_SIZE.get();
            lockRespectDurability = ExcavateServerConfig.LOCK_RESPECT_DURABILITY.get();
            lockRespectHunger = ExcavateServerConfig.LOCK_RESPECT_HUNGER.get();
            lockRespectEnchantments = ExcavateServerConfig.LOCK_RESPECT_ENCHANTMENTS.get();
            lockCollectToPlayer = ExcavateServerConfig.LOCK_COLLECT_TO_PLAYER.get();
            lockToolTier = ExcavateServerConfig.LOCK_TOOL_TIER.get();
            lockPlaceConsumesInventory = ExcavateServerConfig.LOCK_PLACE_CONSUMES_INVENTORY.get();
            lockPlaceReplacesMatching = ExcavateServerConfig.LOCK_PLACE_REPLACES_MATCHING.get();
            lockOreList = ExcavateServerConfig.LOCK_ORE_LIST.get();
            lockAnyList = ExcavateServerConfig.LOCK_ANY_LIST.get();
            harvestReplants = ExcavateServerConfig.HARVEST_REPLANTS.get();
            disabledShapes = new ArrayList<>(ExcavateServerConfig.DISABLED_SHAPES.get());
            disabledMatchModes = new ArrayList<>(ExcavateServerConfig.DISABLED_MATCH_MODES.get());
            useExtraItems = new ArrayList<>(ExcavateServerConfig.USE_EXTRA_ITEMS.get());
            veinBlocksPerTick = ExcavateServerConfig.VEIN_BLOCKS_PER_TICK.get();
            veinTriggerDebounceTicks = ExcavateServerConfig.VEIN_TRIGGER_DEBOUNCE_TICKS.get();
            requiredEnchantment = ExcavateServerConfig.REQUIRED_ENCHANTMENT.get();
            allowFakePlayers = ExcavateServerConfig.ALLOW_FAKE_PLAYERS.get();
            machineShape = ExcavateServerConfig.MACHINE_SHAPE.get();
            machineMatchMode = ExcavateServerConfig.MACHINE_MATCH_MODE.get();
            oreListDefaults = new ArrayList<>(ExcavateServerConfig.ORE_LIST_DEFAULTS.get());
            anyListDefaults = new ArrayList<>(ExcavateServerConfig.ANY_LIST_DEFAULTS.get());
        }

        /** Sets one setting by its config name. Returns false for a name that is not a gate-able setting. */
        boolean set(String key, JsonElement v) {
            switch (key) {
                case "maxVeinSizeCap" -> maxVeinSizeCap = Math.max(1, v.getAsInt());
                case "lockMaxVeinSize" -> lockMaxVeinSize = v.getAsBoolean();
                case "lockRespectDurability" -> lockRespectDurability = v.getAsBoolean();
                case "lockRespectHunger" -> lockRespectHunger = v.getAsBoolean();
                case "lockRespectEnchantments" -> lockRespectEnchantments = v.getAsBoolean();
                case "lockCollectToPlayer" -> lockCollectToPlayer = v.getAsBoolean();
                case "lockToolTier" -> lockToolTier = v.getAsBoolean();
                case "lockPlaceConsumesInventory" -> lockPlaceConsumesInventory = v.getAsBoolean();
                case "lockPlaceReplacesMatching" -> lockPlaceReplacesMatching = v.getAsBoolean();
                case "lockOreList" -> lockOreList = v.getAsBoolean();
                case "lockAnyList" -> lockAnyList = v.getAsBoolean();
                case "harvestReplants" -> harvestReplants = v.getAsBoolean();
                case "disabledShapes" -> disabledShapes = strings(v);
                case "disabledMatchModes" -> disabledMatchModes = strings(v);
                case "useExtraItems" -> useExtraItems = strings(v);
                case "veinBlocksPerTick" -> veinBlocksPerTick = Math.max(1, v.getAsInt());
                case "veinTriggerDebounceTicks" -> veinTriggerDebounceTicks = Math.max(0, v.getAsInt());
                case "requiredEnchantment" -> requiredEnchantment = v.getAsString();
                case "allowFakePlayerVeinMining" -> allowFakePlayers = v.getAsBoolean();
                case "machineShape" -> machineShape = v.getAsString();
                case "machineMatchMode" -> machineMatchMode = v.getAsString();
                case "oreListDefaults" -> oreListDefaults = strings(v);
                case "anyListDefaults" -> anyListDefaults = strings(v);
                default -> {
                    return false;
                }
            }
            return true;
        }

        Resolved done() {
            return new Resolved(maxVeinSizeCap, lockMaxVeinSize, lockRespectDurability, lockRespectHunger,
                    lockRespectEnchantments, lockCollectToPlayer, lockToolTier, lockPlaceConsumesInventory,
                    lockPlaceReplacesMatching, lockOreList, lockAnyList, List.copyOf(disabledShapes),
                    List.copyOf(disabledMatchModes), harvestReplants, List.copyOf(useExtraItems), veinBlocksPerTick,
                    veinTriggerDebounceTicks, requiredEnchantment, allowFakePlayers, machineShape, machineMatchMode,
                    List.copyOf(oreListDefaults), List.copyOf(anyListDefaults));
        }

        private static List<String> strings(JsonElement v) {
            List<String> out = new ArrayList<>();
            if (v.isJsonArray()) v.getAsJsonArray().forEach(e -> out.add(e.getAsString()));
            else out.add(v.getAsString());
            return out;
        }
    }

    private record Tier(String when, String scope, JsonObject set) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "phoenix_excavate_gates.json";
    private static final long CACHE_TICKS = 20L;

    private static final List<Tier> TIERS = new ArrayList<>();
    private static long loadedModified = -1L;
    private static long lastCheckedNanos = 0L;

    private record Cached(long tick, String dimension, Resolved resolved) {}

    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Resolved> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_PLAYER = new ThreadLocal<>();
    private static volatile Resolved clientPolicy;

    private ExcavatePolicy() {}

    // *** The base ***

    /** The server config as it stands, with no tier applied. */
    public static Resolved base() {
        return new Mut().done();
    }

    // *** Tiers ***

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    /** Reads the tier file when it has changed; at most once a second. */
    private static synchronized void reloadIfChanged() {
        long now = System.nanoTime();
        if (now - lastCheckedNanos < 1_000_000_000L && loadedModified != -1L) return;
        lastCheckedNanos = now;
        Path path = file();
        try {
            if (!Files.exists(path)) {
                writeExample(path);
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == loadedModified) return;
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            List<Tier> read = new ArrayList<>();
            if (root.has("tiers")) {
                for (JsonElement e : root.getAsJsonArray("tiers")) {
                    JsonObject o = e.getAsJsonObject();
                    String when = o.has("when") ? o.get("when").getAsString() : "";
                    String scope = o.has("scope") ? o.get("scope").getAsString() : SuiteGates.ANY_SCOPE;
                    JsonObject set = o.has("set") ? o.getAsJsonObject("set") : new JsonObject();
                    for (String key : set.keySet()) {
                        if (!new Mut().set(key, set.get(key))) {
                            PhoenixExcavate.LOGGER.warn("[{}] {}: '{}' is not a setting a tier can change", PhoenixExcavate.MOD_ID,
                                    FILE_NAME, key);
                        }
                    }
                    read.add(new Tier(when, scope, set));
                }
            }
            TIERS.clear();
            TIERS.addAll(read);
            CACHE.clear();
            loadedModified = modified;
            PhoenixExcavate.LOGGER.info("[{}] {} tier(s) from {}", PhoenixExcavate.MOD_ID, TIERS.size(), FILE_NAME);
        } catch (Exception e) {
            PhoenixExcavate.LOGGER.warn("[{}] could not read {}: {}", PhoenixExcavate.MOD_ID, FILE_NAME, e.toString());
            loadedModified = loadedModified == -1L ? 0L : loadedModified;
        }
    }

    private static void writeExample(Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_about", "Tiers raise or lock Phoenix Excavate's settings per player as gates open. 'when' is a " +
                "gate expression (a | b & !c), 'scope' a dimension id or *, and 'set' any of: maxVeinSizeCap, " +
                "lockMaxVeinSize, lockRespectDurability, lockRespectHunger, lockRespectEnchantments, " +
                "lockCollectToPlayer, lockToolTier, lockPlaceConsumesInventory, lockPlaceReplacesMatching, " +
                "lockOreList, lockAnyList, disabledShapes, disabledMatchModes, harvestReplants, useExtraItems, " +
                "veinBlocksPerTick, veinTriggerDebounceTicks, requiredEnchantment, allowFakePlayerVeinMining, machineShape, " +
                "machineMatchMode, oreListDefaults, anyListDefaults. Tiers apply in order, so list " +
                "them from the lowest up. Example (delete the underscore to use it): " +
                "{ \"when\": \"conflux:discipline\", \"set\": { \"maxVeinSizeCap\": 32, \"lockMaxVeinSize\": true } }");
        root.add("tiers", new com.google.gson.JsonArray());
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(root));
    }

    private static boolean scopeMatches(String scope, String dimension) {
        return scope == null || scope.isBlank() || SuiteGates.ANY_SCOPE.equals(scope) || scope.equals(dimension);
    }

    // *** Resolving ***

    /** The settings that apply to this player, in the dimension they are in. */
    public static Resolved resolve(ServerPlayer player) {
        reloadIfChanged();
        String dimension = player.level().dimension().location().toString();
        long now = player.level().getGameTime();
        Cached cached = CACHE.get(player.getUUID());
        if (cached != null && now - cached.tick < CACHE_TICKS && cached.dimension.equals(dimension)) {
            return cached.resolved;
        }
        Mut m = new Mut();
        for (Tier tier : TIERS) {
            if (!scopeMatches(tier.scope, dimension)) continue;
            if (!SuiteGates.isOpen(player, tier.when)) continue;
            for (Map.Entry<String, JsonElement> e : tier.set.entrySet()) {
                try {
                    m.set(e.getKey(), e.getValue());
                } catch (RuntimeException bad) {
                    PhoenixExcavate.LOGGER.warn("[{}] {}: bad value for '{}'", PhoenixExcavate.MOD_ID, FILE_NAME, e.getKey());
                }
            }
        }
        Resolved r = m.done();
        CACHE.put(player.getUUID(), new Cached(now, dimension, r));
        return r;
    }

    public static void forget(UUID player) {
        CACHE.remove(player);
    }

    // *** Which settings the code in front of us should use ***

    /** Runs {@code action} with this player's settings as the current ones. */
    public static void runAs(ServerPlayer player, Runnable action) {
        callAs(player, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAs(ServerPlayer player, Supplier<T> action) {
        Resolved before = CURRENT.get();
        UUID beforePlayer = CURRENT_PLAYER.get();
        CURRENT.set(resolve(player));
        CURRENT_PLAYER.set(player.getUUID());
        try {
            return action.get();
        } finally {
            if (before == null) CURRENT.remove();
            else CURRENT.set(before);
            if (beforePlayer == null) CURRENT_PLAYER.remove();
            else CURRENT_PLAYER.set(beforePlayer);
        }
    }

    /** The player the code in front of us is working for, on the server; null on the client or outside a vein action. */
    public static UUID currentPlayer() {
        return CURRENT_PLAYER.get();
    }

    /**
     * The settings in force. Server code gets the player's when it ran inside {@link #runAs}. On the client, the ones the
     * server last sent; otherwise the base config.
     */
    public static Resolved current() {
        Resolved r = CURRENT.get();
        if (r != null) return r;
        Resolved client = clientPolicy;
        if (client != null && FMLEnvironment.dist.isClient() && !onServerThread()) return client;
        return base();
    }

    private static boolean onServerThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    public static void setClient(Resolved resolved) {
        clientPolicy = resolved;
    }
}
