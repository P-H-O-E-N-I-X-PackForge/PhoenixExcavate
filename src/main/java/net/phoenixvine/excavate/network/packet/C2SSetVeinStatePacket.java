package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenixvine.excavate.vein.VeinMode;
import net.phoenixvine.excavate.vein.VeinServerState;

import java.util.function.Supplier;

public class C2SSetVeinStatePacket {

    private final boolean active;
    private final ResourceLocation shapeId;
    private final String matchModeId;
    private final VeinMode mode;

    public C2SSetVeinStatePacket(boolean active, String shapeId, String matchModeId, VeinMode mode) {
        this.active = active;
        this.shapeId = ResourceLocation.parse(shapeId == null ? "" : shapeId);
        this.matchModeId = matchModeId == null ? "" : matchModeId;
        this.mode = mode == null ? VeinMode.MINE : mode;
    }

    public C2SSetVeinStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.shapeId = ResourceLocation.parse(buf.readUtf());
        this.matchModeId = buf.readUtf();
        this.mode = buf.readEnum(VeinMode.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeUtf(shapeId.toString());
        buf.writeUtf(matchModeId);
        buf.writeEnum(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            VeinServerState.setActive(player.getUUID(), active ? shapeId : null,
                    ResourceLocation.parse(matchModeId), mode);
        });
        ctx.get().setPacketHandled(true);
    }
}
