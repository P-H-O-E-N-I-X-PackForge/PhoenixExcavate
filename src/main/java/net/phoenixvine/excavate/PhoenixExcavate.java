package net.phoenixvine.excavate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
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

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
