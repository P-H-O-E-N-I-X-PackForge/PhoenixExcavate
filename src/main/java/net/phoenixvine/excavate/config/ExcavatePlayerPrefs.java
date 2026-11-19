package net.phoenixvine.excavate.config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each player's client last sent as their preferences. The client's file is the source of truth: it sends them on
 * joining and every time they change, so nothing is stored here between sessions. A player who has not sent any yet
 * gets the defaults.
 */
public final class ExcavatePlayerPrefs {

    private static final Map<UUID, ExcavateSettings> PREFS = new ConcurrentHashMap<>();

    private ExcavatePlayerPrefs() {}

    public static ExcavateSettings of(UUID player) {
        return PREFS.computeIfAbsent(player, id -> new ExcavateSettings());
    }

    public static void forget(UUID player) {
        PREFS.remove(player);
    }

    /** Takes what the client sent. Only the preferences the server uses; the display ones stay on the client. */
    public static void apply(UUID player, boolean respectHunger, boolean generalMiningExhaustion,
                             boolean respectDurability, boolean respectEnchantments, boolean collectToPlayer,
                             boolean respectToolTier, boolean placeConsumesInventory, boolean placeReplacesMatching,
                             int maxVeinSize, boolean includeDiagonalNeighbors) {
        ExcavateSettings s = of(player);
        s.setRespectHunger(respectHunger);
        s.setGeneralMiningExhaustion(generalMiningExhaustion);
        s.setRespectDurability(respectDurability);
        s.setRespectEnchantments(respectEnchantments);
        s.setCollectToPlayer(collectToPlayer);
        s.setRespectToolTier(respectToolTier);
        s.setPlaceConsumesInventory(placeConsumesInventory);
        s.setPlaceReplacesMatching(placeReplacesMatching);
        s.setMaxVeinSize(Math.max(1, Math.min(maxVeinSize, 16096)));
        s.setIncludeDiagonalNeighbors(includeDiagonalNeighbors);
    }
}
