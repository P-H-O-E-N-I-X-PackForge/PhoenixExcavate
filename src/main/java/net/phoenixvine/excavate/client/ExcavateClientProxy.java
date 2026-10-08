package net.phoenixvine.excavate.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.wiki.client.suite.SuiteHudBar;
import net.phoenixvine.wiki.theme.PhoenixTheme;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExcavateClientProxy {

    private ExcavateClientProxy() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ExcavateConfigScreen(screen)));
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        PhoenixExcavate.LOGGER.info("[Phoenix Excavate] Client setup complete.");

        PhoenixTheme.registerMod("net.phoenixvine.excavate", PhoenixExcavate.MOD_ID);

        SuiteHudBar.register(PhoenixExcavate.MOD_ID, SuiteHudBar.PRIORITY_EXCAVATE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PhoenixExcavate.MOD_ID,
                        "textures/gui/suite_bar_icon.png"),
                net.minecraft.network.chat.Component.literal("§fOpen Excavate Settings"),
                () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    net.minecraft.client.gui.screens.Screen current = mc.screen;
                    mc.setScreen(new ExcavateConfigScreen(current));
                });
    }
}
