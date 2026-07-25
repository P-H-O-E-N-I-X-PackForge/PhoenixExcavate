package net.phoenixvine.excavate.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenixvine.excavate.network.packet.C2SSetVeinStatePacket;

import java.util.Optional;

public class ExcavateNetwork {

    private static final String PROTOCOL = "1";

    public static SimpleChannel CHANNEL;
    private static int id = 0;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("phoenix_excavate", "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals);

        CHANNEL.registerMessage(id++,
                C2SSetVeinStatePacket.class,
                C2SSetVeinStatePacket::encode,
                C2SSetVeinStatePacket::new,
                C2SSetVeinStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
