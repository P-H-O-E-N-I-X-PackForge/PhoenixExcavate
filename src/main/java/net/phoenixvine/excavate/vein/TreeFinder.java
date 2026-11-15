package net.phoenixvine.excavate.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public final class TreeFinder {

    private TreeFinder() {}

    public static final TagKey<Block> TREE_LOGS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "tree_logs"));
    public static final TagKey<Block> TREE_FOLIAGE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "tree_foliage"));
    
    public static final TagKey<Block> TREE_ROOTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("phoenix_excavate", "tree_roots"));

    private static final boolean REQUIRE_FOLIAGE = true;

    private static final int MAX_TREES = 24;
    
    private static final int MAX_CANOPY_LEAVES = 4096;
    private static final int MAX_LEAF_HOPS = 6;

    private static final int TREE_HORIZONTAL = 24;
    private static final int TREE_VERTICAL = 80;

    public static boolean isRoot(BlockState state) {
        return state.is(TREE_ROOTS);
    }

    public static boolean isLog(BlockState state) {
        if (isRoot(state)) return false; 
        return state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM) || state.is(TREE_LOGS);
    }

    public static boolean isFoliage(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS)
                || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(TREE_FOLIAGE);
    }

    private record Tree(List<BlockPos> logs, List<BlockPos> roots, List<BlockPos> bases, boolean crown) {}

    public static List<BlockPos> find(BlockGetter level, BlockPos origin, BlockState originState,
                                      int cap, ItemStack tool, boolean respectToolTier) {
        if (!isLog(originState)) return null;

        int limit = Math.max(cap, 1);
        Set<BlockPos> claimed = new HashSet<>();
        Set<BlockPos> queuedSeeds = new HashSet<>();
        PriorityQueue<BlockPos> seeds = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distSqr(origin)));
        seeds.add(origin);
        queuedSeeds.add(origin);

        List<BlockPos> result = new ArrayList<>();
        result.add(origin);
        int trees = 0;
        while (!seeds.isEmpty() && result.size() < limit && trees < MAX_TREES) {
            BlockPos seed = seeds.poll();
            if (claimed.contains(seed)) continue;

            Tree tree = flood(level, seed, claimed, limit - result.size() + 1, tool, respectToolTier);
            if (REQUIRE_FOLIAGE && !tree.crown()) {
                
                if (trees == 0) return null;
                continue; 
            }

            trees++;
            
            List<BlockPos> blocks = new ArrayList<>(tree.logs().size() + tree.roots().size());
            for (BlockPos pos : tree.logs()) if (!pos.equals(origin)) blocks.add(pos);
            blocks.addAll(tree.roots());
            blocks.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
            for (BlockPos pos : blocks) {
                if (result.size() >= limit) break;
                result.add(pos);
            }
            if (result.size() < limit) linkThroughLeaves(level, tree, claimed, queuedSeeds, seeds);
        }
        return result;
    }

    private static Tree flood(BlockGetter level, BlockPos seed, Set<BlockPos> claimed, int cap,
                              ItemStack tool, boolean respectToolTier) {
        List<BlockPos> logs = new ArrayList<>();
        List<BlockPos> roots = new ArrayList<>();
        Set<BlockPos> rootSet = new HashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        logs.add(seed);
        claimed.add(seed);
        seen.add(seed);
        queue.add(seed);
        boolean crown = touchesFoliage(level, seed);

        while (!queue.isEmpty() && logs.size() + roots.size() < cap) {
            BlockPos current = queue.poll();
            boolean currentIsRoot = rootSet.contains(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (logs.size() + roots.size() >= cap) break;

                        BlockPos next = current.offset(dx, dy, dz);
                        if (!seen.add(next) || claimed.contains(next)) continue;

                        if (Math.abs(next.getX() - seed.getX()) > TREE_HORIZONTAL
                                || Math.abs(next.getZ() - seed.getZ()) > TREE_HORIZONTAL
                                || Math.abs(next.getY() - seed.getY()) > TREE_VERTICAL) continue;

                        BlockState candidate = level.getBlockState(next);
                        boolean root = isRoot(candidate);
                        if (!root && !isLog(candidate)) continue;
                        
                        if (root && dy > 0) continue;
                        
                        if (currentIsRoot && !root) continue;
                        if (VeinFinder.isUnbreakable(level, next, candidate)) continue;
                        if (respectToolTier && candidate.requiresCorrectToolForDrops()
                                && !tool.isCorrectToolForDrops(candidate)) continue;

                        claimed.add(next);
                        queue.add(next);
                        if (root) {
                            roots.add(next);
                            rootSet.add(next);
                        } else {
                            logs.add(next);
                            if (!crown && touchesFoliage(level, next)) crown = true;
                        }
                    }
                }
            }
        }

        int minY = Integer.MAX_VALUE;
        for (BlockPos pos : logs) minY = Math.min(minY, pos.getY());
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos pos : logs) {
            if (pos.getY() == minY) bases.add(pos);
        }
        return new Tree(logs, roots, bases, crown);
    }

    private static void linkThroughLeaves(BlockGetter level, Tree tree, Set<BlockPos> claimed,
                                          Set<BlockPos> queuedSeeds, PriorityQueue<BlockPos> seeds) {
        Set<BlockPos> leaves = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Deque<Integer> hops = new ArrayDeque<>();
        for (BlockPos log : tree.logs()) {
            for (Direction dir : Direction.values()) {
                BlockPos next = log.relative(dir);
                if (isFoliage(level.getBlockState(next)) && leaves.add(next)) {
                    queue.add(next);
                    hops.add(1);
                }
            }
        }
        while (!queue.isEmpty() && leaves.size() < MAX_CANOPY_LEAVES) {
            BlockPos leaf = queue.poll();
            int hop = hops.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = leaf.relative(dir);
                BlockState state = level.getBlockState(next);
                if (isLog(state)) {
                    if (claimed.contains(next) || queuedSeeds.contains(next)) continue;
                    BlockState leafState = level.getBlockState(leaf);
                    boolean held = leafState.hasProperty(LeavesBlock.DISTANCE)
                            ? leafState.getValue(LeavesBlock.DISTANCE) == 1 : hop <= 1;
                    if (held && queuedSeeds.add(next)) seeds.add(next);
                } else if (hop < MAX_LEAF_HOPS && isFoliage(state) && leaves.add(next)) {
                    queue.add(next);
                    hops.add(hop + 1);
                }
            }
        }
    }

    private static boolean touchesFoliage(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (isFoliage(level.getBlockState(pos.relative(dir)))) return true;
        }
        return false;
    }
}
