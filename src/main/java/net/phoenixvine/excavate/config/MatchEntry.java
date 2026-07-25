package net.phoenixvine.excavate.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class MatchEntry {

    public enum Type {
        BLOCK,
        TAG
    }

    public Type type = Type.BLOCK;
    public String id = "";

    public MatchEntry() {}

    public MatchEntry(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    public boolean matches(BlockState state) {
        if (id == null || id.isBlank()) return false;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return false;

        if (type == Type.BLOCK) {
            if (!ForgeRegistries.BLOCKS.containsKey(rl)) return false;
            return ForgeRegistries.BLOCKS.getValue(rl) == state.getBlock();
        }

        TagKey<Block> tag = TagKey.create(Registries.BLOCK, rl);
        return state.is(tag);
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }

    public static MatchEntry parse(String raw) {
        if (raw == null) return null;
        int sep = raw.indexOf(':');
        if (sep < 0) return null;
        String typeStr = raw.substring(0, sep).trim().toUpperCase(java.util.Locale.ROOT);
        String id = raw.substring(sep + 1).trim();
        if (id.isEmpty()) return null;
        try {
            return new MatchEntry(Type.valueOf(typeStr), id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
