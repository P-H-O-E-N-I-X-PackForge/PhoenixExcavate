package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.config.ExcavatePlayerPrefs;
import net.phoenixvine.excavate.config.ExcavateSettings;

/** The player's preferences, sent when they join and whenever they change them. */
public class C2SPrefsPacket implements CustomPacketPayload {

    public static final Type<C2SPrefsPacket> TYPE = new Type<>(PhoenixExcavate.id("prefs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPrefsPacket> STREAM_CODEC = StreamCodec
            .of((buf, packet) -> packet.write(buf), C2SPrefsPacket::new);

    private final boolean respectHunger, generalMiningExhaustion, respectDurability, respectEnchantments,
            collectToPlayer, respectToolTier, placeConsumesInventory, placeReplacesMatching, includeDiagonalNeighbors;
    private final int maxVeinSize;

    public C2SPrefsPacket(ExcavateSettings s) {
        respectHunger = s.isRespectHunger();
        generalMiningExhaustion = s.isGeneralMiningExhaustion();
        respectDurability = s.isRespectDurability();
        respectEnchantments = s.isRespectEnchantments();
        collectToPlayer = s.isCollectToPlayer();
        respectToolTier = s.isRespectToolTier();
        placeConsumesInventory = s.isPlaceConsumesInventory();
        placeReplacesMatching = s.isPlaceReplacesMatching();
        includeDiagonalNeighbors = s.isIncludeDiagonalNeighbors();
        maxVeinSize = s.getMaxVeinSize();
    }

    private C2SPrefsPacket(RegistryFriendlyByteBuf buf) {
        respectHunger = buf.readBoolean();
        generalMiningExhaustion = buf.readBoolean();
        respectDurability = buf.readBoolean();
        respectEnchantments = buf.readBoolean();
        collectToPlayer = buf.readBoolean();
        respectToolTier = buf.readBoolean();
        placeConsumesInventory = buf.readBoolean();
        placeReplacesMatching = buf.readBoolean();
        includeDiagonalNeighbors = buf.readBoolean();
        maxVeinSize = buf.readVarInt();
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(respectHunger);
        buf.writeBoolean(generalMiningExhaustion);
        buf.writeBoolean(respectDurability);
        buf.writeBoolean(respectEnchantments);
        buf.writeBoolean(collectToPlayer);
        buf.writeBoolean(respectToolTier);
        buf.writeBoolean(placeConsumesInventory);
        buf.writeBoolean(placeReplacesMatching);
        buf.writeBoolean(includeDiagonalNeighbors);
        buf.writeVarInt(maxVeinSize);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPrefsPacket p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        ExcavatePlayerPrefs.apply(player.getUUID(), p.respectHunger, p.generalMiningExhaustion, p.respectDurability,
                p.respectEnchantments, p.collectToPlayer, p.respectToolTier, p.placeConsumesInventory,
                p.placeReplacesMatching, p.maxVeinSize, p.includeDiagonalNeighbors);
    }
}
