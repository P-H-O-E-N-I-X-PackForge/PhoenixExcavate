package net.phoenixvine.excavate.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.config.ExcavatePolicy;
import net.phoenixvine.excavate.network.packet.S2CPolicyPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sends each player their settings when they join, change dimension, or when a gate changes them. */
@EventBusSubscriber(modid = PhoenixExcavate.MOD_ID)
public final class ExcavatePolicySync {

    private static final Map<UUID, ExcavatePolicy.Resolved> LAST_SENT = new ConcurrentHashMap<>();
    private static final int CHECK_EVERY_TICKS = 40;

    private ExcavatePolicySync() {}

    private static void send(ServerPlayer player, boolean force) {
        ExcavatePolicy.Resolved now = ExcavatePolicy.resolve(player);
        if (!force && now.equals(LAST_SENT.get(player.getUUID()))) return;
        LAST_SENT.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer(player, new S2CPolicyPacket(now));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) send(player, true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExcavatePolicy.forget(player.getUUID());
            send(player, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
        net.phoenixvine.excavate.config.ExcavatePlayerPrefs.forget(event.getEntity().getUUID());
        ExcavatePolicy.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % CHECK_EVERY_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, false);
    }
}
