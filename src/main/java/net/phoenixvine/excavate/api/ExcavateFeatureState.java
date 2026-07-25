package net.phoenixvine.excavate.api;

public enum ExcavateFeatureState {

    DISABLED,
    VISIBLE,
    ENABLED;

    public boolean atLeast(ExcavateFeatureState minimum) {
        return ordinal() >= minimum.ordinal();
    }
}
