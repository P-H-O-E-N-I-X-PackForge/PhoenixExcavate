package net.phoenixvine.excavate.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinMode;
import net.phoenixvine.excavate.vein.VeinServerState;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

@GameTestHolder(PhoenixExcavate.MOD_ID)
@PrefixGameTestTemplate(false)
public class VeinActionGameTests {

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void harvestTakesMatureCropsAndReplantsThem(GameTestHelper helper) {
        MatchModeRegistry.registerBuiltins();
        VeinShapeRegistry.registerBuiltins();

        BlockPos a = new BlockPos(1, 2, 1);
        BlockPos b = new BlockPos(2, 2, 1);
        BlockPos young = new BlockPos(3, 2, 1);
        for (BlockPos pos : new BlockPos[] {a, b, young}) {
            helper.setBlock(pos.below(), Blocks.FARMLAND);
        }
        helper.setBlock(a, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(b, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        helper.setBlock(young, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        VeinServerState.setActive(player.getUUID(), ResourceLocation.parse("phoenix_excavate:blob"),
                ResourceLocation.parse("phoenix_excavate:exact"), VeinMode.HARVEST);

        BlockPos abs = helper.absolutePos(a);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, abs, hit));

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(a, CropBlock.AGE, 0);
            helper.assertBlockProperty(b, CropBlock.AGE, 0);
            helper.assertBlockProperty(young, CropBlock.AGE, 3);
            VeinServerState.setActive(player.getUUID(), null, null, null);
        });
    }

    @GameTest(template = "gametest_empty", timeoutTicks = 100)
    public static void useRepeatsAHoeAcrossConnectedDirt(GameTestHelper helper) {
        MatchModeRegistry.registerBuiltins();
        VeinShapeRegistry.registerBuiltins();

        BlockPos first = new BlockPos(1, 1, 1);
        BlockPos second = new BlockPos(2, 1, 1);
        BlockPos third = new BlockPos(3, 1, 1);
        for (BlockPos pos : new BlockPos[] {first, second, third}) {
            helper.setBlock(pos, Blocks.DIRT);
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(Items.DIAMOND_HOE));
        VeinServerState.setActive(player.getUUID(), ResourceLocation.parse("phoenix_excavate:blob"),
                ResourceLocation.parse("phoenix_excavate:exact"), VeinMode.USE);

        BlockPos abs = helper.absolutePos(first);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, abs, hit));

        helper.succeedWhen(() -> {
            helper.assertBlock(second, b -> b == Blocks.FARMLAND, "second dirt should be tilled");
            helper.assertBlock(third, b -> b == Blocks.FARMLAND, "third dirt should be tilled");
            VeinServerState.setActive(player.getUUID(), null, null, null);
        });
    }
}
