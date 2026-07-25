package net.phoenixvine.excavate.vein;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VeinServerState {

    public record Active(String shapeId, String matchModeId) {}

    private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();

    private VeinServerState() {}

    public static void setActive(UUID player, String shapeId, String matchModeId) {
        if (shapeId == null || shapeId.isBlank()) {
            ACTIVE.remove(player);
        } else {
            ACTIVE.put(player, new Active(shapeId, matchModeId));
        }
    }

    public static void clear(UUID player) {
        ACTIVE.remove(player);
    }

    public static Active active(UUID player) {
        return ACTIVE.get(player);
    }
}
