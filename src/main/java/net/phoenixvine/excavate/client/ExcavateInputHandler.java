package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenixvine.excavate.PhoenixExcavate;
import net.phoenixvine.excavate.config.ExcavateSettings;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ExcavateInputHandler {

    private static boolean prevActivateDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            VeinClientState.setActive(false);

            ExcavateKeyBindings.OPEN_SETTINGS.consumeClick();
            return;
        }

        if (ExcavateKeyBindings.OPEN_SETTINGS.consumeClick()) {
            mc.setScreen(new ExcavateConfigScreen(null));
            return;
        }

        boolean activateDownNow = ExcavateKeyBindings.ACTIVATE.isDown();
        boolean justPressed = activateDownNow && !prevActivateDown;
        prevActivateDown = activateDownNow;

        if (justPressed && Screen.hasControlDown()) {
            VeinClientState.toggleMode();
            while (ExcavateKeyBindings.ACTIVATE.consumeClick()) {} 
            return;
        }

        if (ExcavateSettings.get().isHoldToActivate()) {
            VeinClientState.setActive(activateDownNow);
        } else if (ExcavateKeyBindings.ACTIVATE.consumeClick()) {
            VeinClientState.setActive(!VeinClientState.isActive());
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) return;
        if (!ExcavateKeyBindings.ACTIVATE.isDown() || !Screen.hasShiftDown()) return;

        event.setCanceled(true);
        VeinClientState.cycleShape();
    }
}
