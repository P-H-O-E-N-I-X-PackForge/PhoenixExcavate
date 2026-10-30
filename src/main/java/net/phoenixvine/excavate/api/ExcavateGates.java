package net.phoenixvine.excavate.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenixvine.wiki.api.gate.SuiteGates;

/**
 * Vein features as features of the shared gate API ({@link SuiteGates}). The feature is {@code "excavate:" + id} for
 * every id in {@link ExcavateAPI} (and any id an action registers), so {@code excavate:vein_mining} is bound with
 * {@code SuiteGates.bind(ExcavateGates.feature(ExcavateAPI.FEATURE_VEIN_MINING), "*", "conflux:node/foo")}. A feature
 * that nothing is bound to is allowed. The scope is the dimension id.
 */
public final class ExcavateGates {

    private ExcavateGates() {}

    public static String feature(String featureId) {
        return "excavate:" + featureId;
    }

    public static boolean allowed(ServerPlayer player, String featureId, ResourceLocation dimension) {
        return SuiteGates.allowed(player, feature(featureId), dimension.toString());
    }
}
