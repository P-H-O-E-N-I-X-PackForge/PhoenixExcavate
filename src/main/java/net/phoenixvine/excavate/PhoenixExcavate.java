package net.phoenixvine.excavate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.phoenixvine.excavate.client.ExcavateClientProxy;
import net.phoenixvine.excavate.config.MatchListConfig;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.minecraftforge.fml.config.ModConfig;
import net.phoenixvine.excavate.network.ExcavateNetwork;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;
import net.phoenixvine.wiki.client.suite.SuiteHudBar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PhoenixExcavate.MOD_ID)
@SuppressWarnings("removal")
public class PhoenixExcavate {

    public static final String MOD_ID = "phoenix_excavate";
    public static final Logger LOGGER = LogManager.getLogger();

    public PhoenixExcavate() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ExcavateClientProxy::registerConfigScreen);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ExcavateServerConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[Phoenix Excavate] Bootstrapping vein miner...");
            ExcavateNetwork.init();
            ExcavateSettings.get();
            MatchListConfig.load();
            MatchModeRegistry.registerBuiltins();
            VeinShapeRegistry.registerBuiltins();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("[Phoenix Excavate] Client setup complete.");

        SuiteHudBar.register(MOD_ID, SuiteHudBar.PRIORITY_EXCAVATE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID,
                        "textures/gui/suite_bar_icon.png"),
                net.minecraft.network.chat.Component.literal("§fOpen Excavate Settings"),
                () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    net.minecraft.client.gui.screens.Screen current = mc.screen;
                    mc.setScreen(new net.phoenixvine.excavate.client.ExcavateConfigScreen(current));
                });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
