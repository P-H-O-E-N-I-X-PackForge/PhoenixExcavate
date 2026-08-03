package net.phoenixvine.excavate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenixvine.excavate.PhoenixExcavate;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExcavateSuiteBarClickHandler {

    private ExcavateSuiteBarClickHandler() {}

    static boolean screenWantsBar(Screen screen) {
        if (screen instanceof SuiteHudBarAware) return true;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        Inventory playerInv = mc.player.getInventory();
        for (Slot slot : containerScreen.getMenu().slots) {
            if (slot.container == playerInv) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) return;

        Screen screen = event.getScreen();
        if (!screenWantsBar(screen)) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (ExcavateHud.isPointInSuiteButton(mouseX, mouseY)) {
            event.setCanceled(true);

            Minecraft.getInstance().setScreen(new ExcavateConfigScreen(screen));
        }
    }
}