package net.phoenixvine.excavate.api;

import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.excavate.vein.VeinMatcher;

public record MatchMode(ResourceLocation id, String translationKey, VeinMatcher matcher) {}
