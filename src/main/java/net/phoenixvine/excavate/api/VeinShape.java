package net.phoenixvine.excavate.api;

import net.phoenixvine.excavate.vein.ShapeExpander;

public record VeinShape(String id, String translationKey, ShapeExpander expander, boolean traversesAir) {

    public VeinShape(String id, String translationKey, ShapeExpander expander) {
        this(id, translationKey, expander, false);
    }
}
