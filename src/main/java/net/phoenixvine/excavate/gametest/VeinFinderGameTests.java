package net.phoenixvine.excavate.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.api.MatchMode;
import net.phoenixvine.excavate.api.VeinShape;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinFinder;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

import java.util.List;

@GameTestHolder(PhoenixExcavate.MOD_ID)
@PrefixGameTestTemplate(false)
public class VeinFinderGameTests {

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void findFloodFillsConnectedStoneAndExcludesNonMatchingBlocks(GameTestHelper helper) {
        MatchModeRegistry.registerBuiltins();
        VeinShapeRegistry.registerBuiltins();
        MatchMode exact = MatchModeRegistry.byId("exact");
        VeinShape blob = VeinShapeRegistry.byId("blob");

        BlockPos origin = new BlockPos(1, 1, 1);
        BlockPos armA = origin.offset(1, 0, 0);
        BlockPos armB = origin.offset(1, 1, 0);
        BlockPos dirt = origin.offset(-1, 0, 0);

        helper.setBlock(origin, Blocks.STONE);
        helper.setBlock(armA, Blocks.STONE);
        helper.setBlock(armB, Blocks.STONE);
        helper.setBlock(dirt, Blocks.DIRT);

        List<BlockPos> result = VeinFinder.find(
                helper.getLevel(),
                helper.absolutePos(origin),
                Direction.NORTH,
                exact,
                blob,
                new ItemStack(Items.DIAMOND_PICKAXE)
        );

        helper.assertTrue(result.contains(helper.absolutePos(origin)), "vein should include the origin");
        helper.assertTrue(result.contains(helper.absolutePos(armA)), "vein should include the connected stone at armA");
        helper.assertTrue(result.contains(helper.absolutePos(armB)), "vein should include the connected stone at armB");
        helper.assertTrue(!result.contains(helper.absolutePos(dirt)), "dirt should not join a stone vein under exact match mode");
        helper.assertTrue(result.size() == 3, "expected exactly origin + armA + armB, got " + result.size());

        helper.succeed();
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 200)
    public static void findDoesNotPropagateThroughBedrock(GameTestHelper helper) {
        MatchModeRegistry.registerBuiltins();
        VeinShapeRegistry.registerBuiltins();
        MatchMode exact = MatchModeRegistry.byId("exact");
        VeinShape tunnel = VeinShapeRegistry.byId("tunnel");

        BlockPos origin = new BlockPos(1, 1, 1);
        BlockPos reachable = origin.relative(Direction.EAST);
        BlockPos blocker = reachable.relative(Direction.EAST);
        BlockPos beyond = blocker.relative(Direction.EAST);

        helper.setBlock(origin, Blocks.STONE);
        helper.setBlock(reachable, Blocks.STONE);
        helper.setBlock(blocker, Blocks.BEDROCK);
        helper.setBlock(beyond, Blocks.STONE);

        List<BlockPos> result = VeinFinder.find(helper.getLevel(), helper.absolutePos(origin),
                Direction.EAST, exact, tunnel, new ItemStack(Items.DIAMOND_PICKAXE));

        helper.assertTrue(result.contains(helper.absolutePos(reachable)), "the block before bedrock should be reachable");
        helper.assertTrue(!result.contains(helper.absolutePos(beyond)),
                "bedrock should block further BFS propagation, so the block past it must be unreachable");

        helper.succeed();
    }
}
