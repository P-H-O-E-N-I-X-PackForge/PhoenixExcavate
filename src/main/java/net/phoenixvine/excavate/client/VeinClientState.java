package net.phoenixvine.excavate.client;

import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.network.ExcavateNetwork;
import net.phoenixvine.excavate.network.packet.C2SSetVeinStatePacket;
import net.phoenixvine.excavate.vein.VeinMode;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

public final class VeinClientState {

    private static boolean active = false;
    private static String shapeId = ExcavateSettings.get().getVeinShapeId();
    private static VeinMode mode = loadPersistedMode();

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
        ExcavateSettings settings = ExcavateSettings.get();
        settings.setVeinShapeId(shapeId);
        settings.save();
        if (active) sync();
        ExcavateHud.showShapeToast(next);
    }

    public static void notifyMatchModeChanged() {
        if (active) sync();
    }

    public static VeinMode getMode() {
        return mode;
    }

    public static void toggleMode() {
        mode = mode == VeinMode.MINE ? VeinMode.PLACE : VeinMode.MINE;
        ExcavateSettings settings = ExcavateSettings.get();
        settings.setVeinMode(mode.name());
        settings.save();
        if (active) sync();
        ExcavateHud.showModeToast(mode);
    }

    private static VeinMode loadPersistedMode() {
        try {
            return VeinMode.valueOf(ExcavateSettings.get().getVeinMode());
        } catch (IllegalArgumentException e) {
            return VeinMode.MINE;
        }
    }

    private static void sync() {
        if (ExcavateNetwork.CHANNEL != null) {
            ExcavateNetwork.CHANNEL.sendToServer(
                    new C2SSetVeinStatePacket(active, shapeId, ExcavateSettings.get().getMatchModeId(), mode));
        }
    }
}
