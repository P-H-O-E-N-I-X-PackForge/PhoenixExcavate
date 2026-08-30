package net.phoenixvine.excavate.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchEntryTest {

    @Test
    void parsesBlockEntry() {
        MatchEntry e = MatchEntry.parse("BLOCK:minecraft:stone");
        assertEquals(MatchEntry.Type.BLOCK, e.type);
        assertEquals("minecraft:stone", e.id);
    }

    @Test
    void parsesTagEntry() {
        MatchEntry e = MatchEntry.parse("TAG:forge:ores");
        assertEquals(MatchEntry.Type.TAG, e.type);
        assertEquals("forge:ores", e.id);
    }

    @Test
    void typeIsCaseInsensitive() {
        MatchEntry e = MatchEntry.parse("block:minecraft:stone");
        assertEquals(MatchEntry.Type.BLOCK, e.type);
    }

    @Test
    void trimsWhitespaceAroundTypeAndId() {
        MatchEntry e = MatchEntry.parse(" BLOCK :  minecraft:stone ");
        assertEquals(MatchEntry.Type.BLOCK, e.type);
        assertEquals("minecraft:stone", e.id);
    }

    @Test
    void onlyFirstColonSeparatesTypeFromId() {
        MatchEntry e = MatchEntry.parse("TAG:forge:ores:extra");
        assertEquals("forge:ores:extra", e.id, "everything after the first colon belongs to the id");
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(MatchEntry.parse(null));
    }

    @Test
    void returnsNullWhenNoColonPresent() {
        assertNull(MatchEntry.parse("minecraft:stone"));
    }

    @Test
    void returnsNullForUnknownType() {
        assertNull(MatchEntry.parse("FLUID:minecraft:water"));
    }

    @Test
    void returnsNullForEmptyId() {
        assertNull(MatchEntry.parse("BLOCK:"));
        assertNull(MatchEntry.parse("BLOCK:   "));
    }

    @Test
    void toStringRoundTripsThroughParse() {
        MatchEntry original = new MatchEntry(MatchEntry.Type.TAG, "forge:ores");
        MatchEntry reparsed = MatchEntry.parse(original.toString());
        assertEquals(original.type, reparsed.type);
        assertEquals(original.id, reparsed.id);
    }
}
