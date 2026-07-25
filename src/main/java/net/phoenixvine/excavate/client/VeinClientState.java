package net.phoenixvine.excavate.client;

import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.network.ExcavateNetwork;
import net.phoenixvine.excavate.network.packet.C2SSetVeinStatePacket;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

public final class VeinClientState {

    private static boolean active = false;
    private static String shapeId = "shapeless";

    private VeinClientState() {}

    public static boolean isActive() {
        return active;
    }

    public static String getShapeId() {
        return shapeId;
    }

    public static VeinShape getShape() {
        return VeinShapeRegistry.byId(shapeId);
    }

    public static void setActive(boolean newActive) {
        if (newActive == active) return;
        active = newActive;
        sync();
    }

    public static void cycleShape() {
        VeinShape next = VeinShapeRegistry.next(shapeId);
        if (next == null) return;
        shapeId = next.id();
        if (active) sync();
        ExcavateHud.showShapeToast(next);
    }

    public static void notifyMatchModeChanged() {
        if (active) sync();
    }

    private static void sync() {
        if (ExcavateNetwork.CHANNEL != null) {
            ExcavateNetwork.CHANNEL.sendToServer(
                    new C2SSetVeinStatePacket(active, shapeId, ExcavateSettings.get().getMatchModeId()));
        }
    }
}
