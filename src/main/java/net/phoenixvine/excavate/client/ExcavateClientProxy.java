package net.phoenixvine.excavate.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.wiki.client.suite.SuiteHudBar;
import net.phoenixvine.wiki.theme.PhoenixTheme;

@EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, value = Dist.CLIENT)
public final class ExcavateClientProxy {

    private ExcavateClientProxy() {}

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, screen) -> new ExcavateConfigScreen(screen));
    }

    /** Sends this player's preferences to the server they are on, if they are on one. */
    public static void sendPreferences() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.phoenixvine.excavate.network.packet.C2SPrefsPacket(
                        net.phoenixvine.excavate.config.ExcavateSettings.get()));
    }

    @SubscribeEvent
    public static void onLoggedIn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        sendPreferences();
    }

    @SubscribeEvent
    public static void onLoggedOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        net.phoenixvine.excavate.config.ExcavatePolicy.setClient(null);
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        PhoenixExcavate.LOGGER.info("[Phoenix Excavate] Client setup complete.");

        PhoenixTheme.registerMod("net.phoenixvine.excavate", PhoenixExcavate.MOD_ID);

        SuiteHudBar.register(PhoenixExcavate.MOD_ID, SuiteHudBar.PRIORITY_EXCAVATE,
                ResourceLocation.fromNamespaceAndPath(PhoenixExcavate.MOD_ID, "textures/gui/suite_bar_icon.png"),
                net.minecraft.network.chat.Component.translatable("phoenix_excavate.ui.open_settings"),
                () -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    mc.setScreen(new ExcavateConfigScreen(mc.screen));
                });
    }
}
