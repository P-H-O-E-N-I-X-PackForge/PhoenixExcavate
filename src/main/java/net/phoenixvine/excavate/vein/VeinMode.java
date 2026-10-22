package net.phoenixvine.excavate.vein;

public enum VeinMode {
    MINE("Mine"),
    PLACE("Place"),
    REPLACE("Replace");

    private final String label;

    VeinMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public VeinMode next() {
        VeinMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
