package net.phoenixvine.excavate.vein;

public enum VeinMode {
    MINE("Mine", "phoenix_excavate.mode.mine"),
    PLACE("Place", "phoenix_excavate.mode.place"),
    REPLACE("Replace", "phoenix_excavate.mode.replace");

    private final String label;
    private final String translationKey;

    VeinMode(String label, String translationKey) {
        this.label = label;
        this.translationKey = translationKey;
    }

    public String label() {
        return label;
    }

    public String translationKey() {
        return translationKey;
    }

    public VeinMode next() {
        VeinMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
