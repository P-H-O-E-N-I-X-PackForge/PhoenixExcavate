package net.phoenixvine.excavate.api;

import net.minecraft.resources.ResourceLocation;
import net.phoenixvine.excavate.vein.VeinMatcher;

/**
 *
 * @param id the resloc id of the match mode
 * @param translationKey the resloc of the match mode's translation
 * @param matcher implementation of the VeinMatcher interface
 * @see VeinMatcher
 */
public record MatchMode(ResourceLocation id, String translationKey, VeinMatcher matcher) {}
