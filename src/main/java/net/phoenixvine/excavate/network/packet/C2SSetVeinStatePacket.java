package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.vein.VeinMode;
import net.phoenixvine.excavate.vein.VeinServerState;

public class C2SSetVeinStatePacket implements CustomPacketPayload {

    public static final Type<C2SSetVeinStatePacket> TYPE = new Type<>(PhoenixExcavate.id("set_vein_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetVeinStatePacket> STREAM_CODEC = StreamCodec
            .of((buf, packet) -> packet.encode(buf), C2SSetVeinStatePacket::new);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SSetVeinStatePacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        VeinServerState.setActive(player.getUUID(), packet.active ? packet.shapeId : null,
                ResourceLocation.parse(packet.matchModeId), packet.mode);
    }
}
