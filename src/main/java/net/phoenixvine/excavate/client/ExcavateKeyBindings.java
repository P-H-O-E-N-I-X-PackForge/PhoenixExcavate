package net.phoenixvine.excavate.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenixvine.excavate.PhoenixExcavate;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ExcavateKeyBindings {

    public static final KeyMapping ACTIVATE = new KeyMapping(
            "key.phoenix_excavate.activate",
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.phoenix_excavate");

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.phoenix_excavate.open_settings",
            GLFW.GLFW_KEY_O,
            "key.categories.phoenix_excavate");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE);
        event.register(OPEN_SETTINGS);
    }
}
