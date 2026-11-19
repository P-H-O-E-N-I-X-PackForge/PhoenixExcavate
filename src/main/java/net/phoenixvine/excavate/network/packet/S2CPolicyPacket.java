package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.phoenixvine.excavate.config.ExcavatePolicy;

import java.util.function.Supplier;

/** The settings that apply to the player now (see {@link ExcavatePolicy}), so the screen can show what is locked. */
public class S2CPolicyPacket {

    private final ExcavatePolicy.Resolved policy;

    public S2CPolicyPacket(ExcavatePolicy.Resolved policy) {
        this.policy = policy;
    }

    public S2CPolicyPacket(FriendlyByteBuf buf) {
        this.policy = ExcavatePolicy.Resolved.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        policy.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ExcavatePolicy.setClient(policy));
        ctx.get().setPacketHandled(true);
    }
}
