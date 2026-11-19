package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.config.ExcavatePolicy;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.network.ExcavateNetwork;
import net.phoenixvine.excavate.network.packet.C2SPrefsPacket;

/** Tells the server this player's preferences when they join and when they change, and forgets its settings on leaving. */
@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExcavatePrefsClient {

    private ExcavatePrefsClient() {}

    /** Sends this player's preferences to the server they are on, if they are on one. */
    public static void sendPreferences() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null || ExcavateNetwork.CHANNEL == null) return;
        ExcavateNetwork.CHANNEL.sendToServer(new C2SPrefsPacket(ExcavateSettings.get()));
    }

    @SubscribeEvent
    public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        sendPreferences();
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ExcavatePolicy.setClient(null);
    }
}
