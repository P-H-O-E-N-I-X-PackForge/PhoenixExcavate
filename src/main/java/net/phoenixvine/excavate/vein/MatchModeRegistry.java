package net.phoenixvine.excavate.vein;

import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.config.MatchEntry;
import net.phoenixvine.excavate.config.MatchListConfig;
import net.phoenixvine.excavate.config.ExcavateServerConfig;

import java.util.ArrayList;
import java.util.List;

public final class MatchModeRegistry {

    private static final List<MatchMode> MODES = new ArrayList<>();

    private MatchModeRegistry() {}

    public static void registerBuiltins() {
        MODES.clear();
        MODES.add(new MatchMode("exact", "phoenix_excavate.matchmode.exact",
                (origin, candidate) -> candidate.getBlock() == origin.getBlock()));

        MODES.add(new MatchMode("match_ore", "phoenix_excavate.matchmode.match_ore",
                (origin, candidate) -> {
                    List<MatchEntry> list = MatchListConfig.get("ore");
                    return matchesAny(list, origin) && matchesAny(list, candidate);
                }));

        MODES.add(new MatchMode("match_any", "phoenix_excavate.matchmode.match_any",
                (origin, candidate) -> {
                    List<MatchEntry> anyList = MatchListConfig.get("any");
                    List<MatchEntry> oreList = MatchListConfig.get("ore");
                    return matchesAny(anyList, candidate) || !matchesAny(oreList, candidate);
                }));
    }

    public static void register(MatchMode mode) {
        MODES.removeIf(m -> m.id().equals(mode.id()));
        MODES.add(mode);
    }

    public static List<MatchMode> all() {
        return List.copyOf(MODES);
    }

    public static List<MatchMode> allEnabled() {
        List<MatchMode> out = new ArrayList<>();
        for (MatchMode m : MODES) if (ExcavateServerConfig.isMatchModeAllowed(m.id())) out.add(m);
        return out;
    }

    public static MatchMode byId(String id) {
        for (MatchMode m : MODES) if (m.id().equals(id)) return m;
        return MODES.isEmpty() ? null : MODES.get(0);
    }

    public static MatchMode next(String currentId) {
        List<MatchMode> enabled = allEnabled();
        if (enabled.isEmpty()) return null;
        int idx = 0;
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).id().equals(currentId)) {
                idx = i;
                break;
            }
        }
        return enabled.get((idx + 1) % enabled.size());
    }

    private static boolean matchesAny(List<MatchEntry> list, net.minecraft.world.level.block.state.BlockState state) {
        for (MatchEntry e : list) if (e.matches(state)) return true;
        return false;
    }
}
