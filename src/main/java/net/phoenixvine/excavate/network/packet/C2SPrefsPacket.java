package net.phoenixvine.excavate.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenixvine.excavate.config.ExcavatePlayerPrefs;
import net.phoenixvine.excavate.config.ExcavateSettings;

import java.util.function.Supplier;

/** The player's preferences, sent when they join and whenever they change them. */
public class C2SPrefsPacket {

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

    public C2SPrefsPacket(FriendlyByteBuf buf) {
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

    public void encode(FriendlyByteBuf buf) {
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ExcavatePlayerPrefs.apply(player.getUUID(), respectHunger, generalMiningExhaustion, respectDurability,
                    respectEnchantments, collectToPlayer, respectToolTier, placeConsumesInventory,
                    placeReplacesMatching, maxVeinSize, includeDiagonalNeighbors);
        });
        ctx.get().setPacketHandled(true);
    }
}
