package net.phoenixvine.excavate.vein;

import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

public class VeinUtil {
    public record ActiveConfig(VeinShape shape, MatchMode matchMode) {}

    public static ActiveConfig getValidatedConfig(VeinServerState.Active active) {
        if (active == null) return null;

        String shapeId = active.shapeId().toString();
        String matchModeId = active.matchModeId().toString();

        if (!ExcavateServerConfig.isShapeAllowed(shapeId) ||
                !ExcavateServerConfig.isMatchModeAllowed(matchModeId)) {
            return null;
        }

        VeinShape shape = VeinShapeRegistry.byId(shapeId);
        MatchMode matchMode = MatchModeRegistry.byId(matchModeId);

        if (shape == null || matchMode == null) return null;

        return new ActiveConfig(shape, matchMode);
    }
}