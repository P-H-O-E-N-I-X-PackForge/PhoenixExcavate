package net.phoenixvine.excavate.api;

import net.phoenixvine.excavate.vein.VeinMatcher;

public record MatchMode(String id, String translationKey, VeinMatcher matcher) {}
