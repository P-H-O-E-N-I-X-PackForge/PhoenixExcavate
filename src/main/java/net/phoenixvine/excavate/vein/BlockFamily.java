package net.phoenixvine.excavate.vein;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.List;

public final class BlockFamily {

    private BlockFamily() {}

    private static final List<TagKey<Block>> STONE_TAGS = List.of(
            BlockTags.BASE_STONE_OVERWORLD,
            BlockTags.BASE_STONE_NETHER,
            BlockTags.STONE_ORE_REPLACEABLES,
            BlockTags.DEEPSLATE_ORE_REPLACEABLES,
            BlockTags.STONE_BRICKS,
            Tags.Blocks.STONE,
            Tags.Blocks.COBBLESTONE,
            Tags.Blocks.NETHERRACK
    );

    private static final List<TagKey<Block>> OTHER_FAMILY_TAGS = List.of(
            BlockTags.LOGS,
            BlockTags.PLANKS,
            BlockTags.LEAVES,
            BlockTags.WOOL,
            BlockTags.DIRT,
            BlockTags.SAND,
            BlockTags.ICE,
            BlockTags.TERRACOTTA
    );

    public static boolean sameFamily(BlockState origin, BlockState candidate) {
        if (origin.getBlock() == candidate.getBlock()) return true;

        if (isStone(origin) && isStone(candidate)) {
            return true;
        }

        for (TagKey<Block> tag : OTHER_FAMILY_TAGS) {
            if (origin.is(tag) && candidate.is(tag)) return true;
        }

        return false;
    }

    private static boolean isStone(BlockState state) {
        for (TagKey<Block> tag : STONE_TAGS) {
            if (state.is(tag)) return true;
        }
        return false;
    }
}