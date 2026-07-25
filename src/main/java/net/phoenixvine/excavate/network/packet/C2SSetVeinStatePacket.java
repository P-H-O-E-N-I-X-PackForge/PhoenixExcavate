package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenixvine.excavate.vein.VeinServerState;

import java.util.function.Supplier;

public class C2SSetVeinStatePacket {

    private final boolean active;
    private final String shapeId;
    private final String matchModeId;

    public C2SSetVeinStatePacket(boolean active, String shapeId, String matchModeId) {
        this.active = active;
        this.shapeId = shapeId == null ? "" : shapeId;
        this.matchModeId = matchModeId == null ? "" : matchModeId;
    }

    public C2SSetVeinStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.shapeId = buf.readUtf();
        this.matchModeId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeUtf(shapeId);
        buf.writeUtf(matchModeId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            VeinServerState.setActive(player.getUUID(), active ? shapeId : null, matchModeId);
        });
        ctx.get().setPacketHandled(true);
    }
}
