package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.config.ExcavatePolicy;

/** The settings that apply to the player now (see {@link ExcavatePolicy}), so the screen can show what is locked. */
public class S2CPolicyPacket implements CustomPacketPayload {

    public static final Type<S2CPolicyPacket> TYPE = new Type<>(PhoenixExcavate.id("policy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPolicyPacket> STREAM_CODEC = StreamCodec
            .of((buf, packet) -> packet.policy.write(buf), buf -> new S2CPolicyPacket(ExcavatePolicy.Resolved.read(buf)));

    private final ExcavatePolicy.Resolved policy;

    public S2CPolicyPacket(ExcavatePolicy.Resolved policy) {
        this.policy = policy;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CPolicyPacket packet, IPayloadContext ctx) {
        ExcavatePolicy.setClient(packet.policy);
    }
}
