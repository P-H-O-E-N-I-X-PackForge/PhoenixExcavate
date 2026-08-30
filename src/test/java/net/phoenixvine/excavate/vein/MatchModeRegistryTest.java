package net.phoenixvine.excavate.vein;

import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.excavate.api.MatchMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MatchModeRegistryTest {

    @BeforeEach
    void registerModes() {
        MatchModeRegistry.registerBuiltins();
    }

    @Test
    void registerBuiltinsRegistersAllThreeModes() {
        Set<String> ids = MatchModeRegistry.all().stream()
                .map(m -> m.id().getPath())
                .collect(Collectors.toSet());
    }

    @Test
    void registerBuiltinsClearsPreviousRegistrations() {
        MatchModeRegistry.register(new MatchMode(ResourceLocation.parse("phoenix_excavate:custom"), "custom.key", (origin, candidate) -> true));
        assertNotNull(MatchModeRegistry.byId("custom"));

        MatchModeRegistry.registerBuiltins();

        List<MatchMode> all = MatchModeRegistry.all();
        assertFalse(all.stream().anyMatch(m -> m.id().equals("custom")));
        assertEquals(3, all.size());
    }

    @Test
    void registerReplacesAnExistingModeWithTheSameId() {
        MatchMode replacement = new MatchMode(ResourceLocation.parse("phoenix_excavate:exact"), "phoenix_excavate.matchmode.exact.v2", (origin, candidate) -> false);
        MatchModeRegistry.register(replacement);

        assertSame(replacement, MatchModeRegistry.byId("exact"));
        assertEquals(3, MatchModeRegistry.all().size(), "replacing by id should not grow the registry");
    }

    @Test
    void byIdFallsBackToFirstRegisteredModeForUnknownId() {
        MatchMode fallback = MatchModeRegistry.byId("does-not-exist");
        assertEquals(MatchModeRegistry.all().get(0), fallback);
    }
}
