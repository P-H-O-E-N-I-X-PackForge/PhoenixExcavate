package net.phoenixvine.excavate.vein;

import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.excavate.api.ExcavateAPI;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles the server side active state for the shape and match mode.
 *
 * <p> Sets the active state or removes it per player.
 *
 * @see VeinShapeRegistry
 * @apiNote This class is for internal use only. Addons should not be able to mutate the server state directly.
 *
 */
public final class VeinServerState {

    public record Active(ResourceLocation shapeId, ResourceLocation matchModeId, VeinMode mode) {}

    private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();

    private VeinServerState() {}

    public static void setActive(UUID player, ResourceLocation shapeId, ResourceLocation matchModeId,
                                 VeinMode mode) {
        if (shapeId == null) {
            ACTIVE.remove(player);
        } else {
            ACTIVE.put(player, new Active(shapeId, matchModeId, mode == null ? VeinMode.MINE : mode));
        }
    }

    public static Active active(UUID player) {
        return ACTIVE.get(player);
    }
}
