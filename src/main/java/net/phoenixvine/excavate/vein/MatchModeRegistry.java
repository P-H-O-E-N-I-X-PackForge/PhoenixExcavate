package net.phoenixvine.excavate.vein;

import net.minecraft.resources.ResourceLocation;
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
        MODES.add(new MatchMode(
                ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "exact"),
                "phoenix_excavate.matchmode.exact",
                (origin, candidate) -> candidate.getBlock() == origin.getBlock()
        ));

        MODES.add(new MatchMode(
                ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "match_ore"),
                "phoenix_excavate.matchmode.match_ore",
                (origin, candidate) -> {
                    List<MatchEntry> list = MatchListConfig.get("ore");
                    return matchesAny(list, origin) && matchesAny(list, candidate);
                }
        ));

        MODES.add(new MatchMode(
                ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "match_any"),
                "phoenix_excavate.matchmode.match_any",
                (origin, candidate) -> {
                    List<MatchEntry> anyList = MatchListConfig.get("any");
                    List<MatchEntry> oreList = MatchListConfig.get("ore");
                    return matchesAny(anyList, candidate) || !matchesAny(oreList, candidate);
                }
        ));
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
        for (MatchMode m : MODES) {
            if (ExcavateServerConfig.isMatchModeAllowed(m.id().toString())) {
                out.add(m);
            }
        }
        return out;
    }

    private static String path(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    public static MatchMode byId(String id) {
        String path = path(id);
        for (MatchMode m : MODES) {
            if (m.id().getPath().equals(path)) return m;
        }
        return MODES.isEmpty() ? null : MODES.get(0);
    }

    public static MatchMode next(String currentId) {
        List<MatchMode> enabled = allEnabled();
        if (enabled.isEmpty()) return null;
        String path = path(currentId);
        int idx = 0;
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).id().getPath().equals(path)) {
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