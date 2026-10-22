package net.phoenixvine.excavate;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.phoenixvine.excavate.client.ExcavateClientProxy;
import net.phoenixvine.excavate.config.ExcavateServerConfig;
import net.phoenixvine.excavate.config.ExcavateSettings;
import net.phoenixvine.excavate.config.MatchListConfig;
import net.phoenixvine.excavate.network.ExcavateNetwork;
import net.phoenixvine.excavate.vein.MatchModeRegistry;
import net.phoenixvine.excavate.vein.VeinShapeRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PhoenixExcavate.MOD_ID)
public class PhoenixExcavate {

    public static final String MOD_ID = "phoenix_excavate";
    public static final Logger LOGGER = LogManager.getLogger();

    public PhoenixExcavate(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ExcavateNetwork::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ExcavateClientProxy.registerConfigScreen(container);
        }

        container.registerConfig(ModConfig.Type.SERVER, ExcavateServerConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[Phoenix Excavate] Bootstrapping vein miner...");
            ExcavateSettings.get();
            MatchListConfig.load();
            MatchModeRegistry.registerBuiltins();
            VeinShapeRegistry.registerBuiltins();
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
