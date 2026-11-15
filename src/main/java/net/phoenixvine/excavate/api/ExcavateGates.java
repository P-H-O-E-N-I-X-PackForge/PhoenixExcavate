package net.phoenixvine.excavate.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenixvine.wiki.api.gate.SuiteGates;

public final class ExcavateGates {

    private ExcavateGates() {}

    public static String feature(String featureId) {
        return "excavate:" + featureId;
    }

    public static boolean allowed(ServerPlayer player, String featureId, ResourceLocation dimension) {
        return SuiteGates.allowed(player, feature(featureId), dimension.toString());
    }
}
