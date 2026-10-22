package net.phoenixvine.excavate.network;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.phoenixvine.excavate.network.packet.C2SSetVeinStatePacket;

public class ExcavateNetwork {

    private static final String PROTOCOL = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(C2SSetVeinStatePacket.TYPE, C2SSetVeinStatePacket.STREAM_CODEC,
                C2SSetVeinStatePacket::handle);
    }

    public static void sendToServer(C2SSetVeinStatePacket packet) {
        PacketDistributor.sendToServer(packet);
    }
}
