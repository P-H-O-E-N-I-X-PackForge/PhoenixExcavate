package net.phoenixvine.excavate.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinServerState;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class ExcavateAPI {

    private ExcavateAPI() {}

    public static void registerShape(VeinShape shape) {
        VeinShapeRegistry.register(shape);
    }

    public static List<VeinShape> getShapes() {
        return VeinShapeRegistry.all();
    }

    public static void registerMatchMode(MatchMode mode) {
        MatchModeRegistry.register(mode);
    }

    public static List<MatchMode> getMatchModes() {
        return MatchModeRegistry.all();
    }

    public static boolean isActive(Player player) {
        return VeinServerState.active(player.getUUID()) != null;
    }

    public static VeinShape getActiveShape(Player player) {
        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        return active == null ? null : VeinShapeRegistry.byId(active.shapeId());
    }

    public static MatchMode getActiveMatchMode(Player player) {
        VeinServerState.Active active = VeinServerState.active(player.getUUID());
        return active == null ? null : MatchModeRegistry.byId(active.matchModeId());
    }

    public static final String FEATURE_VEIN_MINING = "vein_mining";

    private static final Map<String, BooleanSupplier> FEATURE_GATES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> DIMENSION_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<ResourceLocation, Integer>> TIER_REQUIREMENTS = new ConcurrentHashMap<>();
    private static final Map<String, Map<ResourceLocation, ExcavateFeatureState>> FEATURE_STATES =
            new ConcurrentHashMap<>();

    private static final Set<String> KNOWN_FEATURE_IDS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_UNKNOWN_FEATURE_IDS = ConcurrentHashMap.newKeySet();

    static {
        KNOWN_FEATURE_IDS.add(FEATURE_VEIN_MINING);
    }

    public static void registerFeatureGate(String featureId, BooleanSupplier check) {
        KNOWN_FEATURE_IDS.add(featureId);
        FEATURE_GATES.put(featureId, check);
    }

    public static void setFeatureEnabled(String featureId, boolean enabled) {
        registerFeatureGate(featureId, () -> enabled);
    }

    public static void clearFeatureGate(String featureId) {
        FEATURE_GATES.remove(featureId);
    }

    public static void setTier(ResourceLocation dimension, int tier) {
        DIMENSION_TIERS.put(dimension, tier);
    }

    public static int getTier(ResourceLocation dimension) {
        return DIMENSION_TIERS.getOrDefault(dimension, 0);
    }

    public static void requireTier(String featureId, ResourceLocation dimension, int requiredTier) {
        KNOWN_FEATURE_IDS.add(featureId);
        TIER_REQUIREMENTS.computeIfAbsent(featureId, id -> new ConcurrentHashMap<>()).put(dimension, requiredTier);
    }

    public static void clearTierRequirement(String featureId, ResourceLocation dimension) {
        Map<ResourceLocation, Integer> perDimension = TIER_REQUIREMENTS.get(featureId);
        if (perDimension != null) perDimension.remove(dimension);
    }

    public static boolean isFeatureEnabled(String featureId, ResourceLocation dimension) {
        warnIfUnknown(featureId);
        if (!checkGate(featureId)) return false;

        Map<ResourceLocation, Integer> perDimension = TIER_REQUIREMENTS.get(featureId);
        if (perDimension == null) return true;

        Integer required = perDimension.get(dimension);
        return required == null || getTier(dimension) >= required;
    }

    private static boolean checkGate(String featureId) {
        BooleanSupplier check = FEATURE_GATES.get(featureId);
        if (check == null) return true;
        return check.getAsBoolean();
    }

    private static void warnIfUnknown(String featureId) {
        if (!KNOWN_FEATURE_IDS.contains(featureId) && WARNED_UNKNOWN_FEATURE_IDS.add(featureId)) {
            PhoenixExcavate.LOGGER.debug(
                    "Feature id '{}' was queried but has never been gated, tiered, or given an explicit" +
                            " state - defaulting to enabled. Fine if that's intentional; if not, check for a" +
                            " typo against whatever was supposed to configure it.",
                    featureId);
        }
    }

    public static void setFeatureState(String featureId, ResourceLocation dimension, ExcavateFeatureState state) {
        KNOWN_FEATURE_IDS.add(featureId);
        FEATURE_STATES.computeIfAbsent(featureId, id -> new ConcurrentHashMap<>()).put(dimension, state);
    }

    public static ExcavateFeatureState getFeatureState(String featureId, ResourceLocation dimension) {
        warnIfUnknown(featureId);
        Map<ResourceLocation, ExcavateFeatureState> perDimension = FEATURE_STATES.get(featureId);
        return perDimension == null ? ExcavateFeatureState.ENABLED :
                perDimension.getOrDefault(dimension, ExcavateFeatureState.ENABLED);
    }
}
