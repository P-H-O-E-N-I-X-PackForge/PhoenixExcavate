package net.phoenixvine.excavate.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.phoenixvine.excavate.PhoenixExcavate;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, value = Dist.CLIENT)
public class ExcavateKeyBindings {

    public static final KeyMapping ACTIVATE = new KeyMapping(
            "key.phoenix_excavate.activate",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.phoenix_excavate");

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.phoenix_excavate.open_settings",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O,
            "key.categories.phoenix_excavate");

    public static final KeyMapping CYCLE_MODE = new KeyMapping(
            "key.phoenix_excavate.cycle_mode",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.phoenix_excavate");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE);
        event.register(OPEN_SETTINGS);
        event.register(CYCLE_MODE);
    }
}
